package net.evendanan.bazel.mvn;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Charsets;

import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.GraphMerger;
import net.evendanan.bazel.mvn.api.RuleWriter;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.ExportsGenerationType;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;
import net.evendanan.bazel.mvn.api.model.ResolutionOutput;
import net.evendanan.bazel.mvn.api.model.TargetType;
import net.evendanan.bazel.mvn.api.serialization.Serialization;
import net.evendanan.bazel.mvn.impl.RuleWriters;
import net.evendanan.bazel.mvn.impl.TargetsBuilderForType;
import net.evendanan.bazel.mvn.impl.TargetsBuilders;
import net.evendanan.bazel.mvn.merger.ArtifactDownloader;
import net.evendanan.bazel.mvn.merger.ClearSrcJarAttribute;
import net.evendanan.bazel.mvn.merger.DefaultMerger;
import net.evendanan.bazel.mvn.merger.DependencyToolsWithPrefix;
import net.evendanan.bazel.mvn.merger.GraphVerifications;
import net.evendanan.bazel.mvn.merger.PublicTargetsCategory;
import net.evendanan.bazel.mvn.merger.SourcesJarLocator;
import net.evendanan.bazel.mvn.merger.TargetCommenter;
import net.evendanan.bazel.mvn.merger.TestOnlyMarker;
import net.evendanan.timing.ProgressTimer;

import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static net.evendanan.bazel.mvn.merger.GraphUtils.DfsTraveller;

public class Merger {

    private static final String NEW_LINE = System.lineSeparator();

    private final GraphMerger merger;
    private final RuleWriter repositoryRulesMacroWriter;
    private final RuleWriter targetsMacroWriter;
    private final RuleWriter hardAliasesWriter;
    private final File macrosFile;
    private final File actualMacrosFile;

    private Merger(final CommandLineOptions options) throws IOException {
        final File parent = new File(options.output_target_build_files_base_path);
        actualMacrosFile = new File(parent, options.output_macro_file);
        macrosFile = File.createTempFile("temp_mabel_deps", actualMacrosFile.getName());

        merger = new DefaultMerger(options.version_conflict_resolver.createMerger());
        repositoryRulesMacroWriter =
                new RuleWriters.HttpRepoRulesMacroWriter(macrosFile, "generate_workspace_rules");
        targetsMacroWriter =
                new RuleWriters.TransitiveRulesMacroWriter(
                        macrosFile, "generate_transitive_dependency_targets");

        if (options.create_deps_sub_folders) {
            if (options.package_path.isEmpty()) {
                throw new IllegalArgumentException(
                        "--package_path can not be empty, if --output_target_build_files_base_path was set.");
            }
            hardAliasesWriter =
                    new RuleWriters.TransitiveRulesAliasWriter(parent, options.package_path);
        } else {
            hardAliasesWriter = dependencies -> {
            };
        }
    }

    public static void main(String[] args) throws Exception {
        CommandLineOptions options = new CommandLineOptions();
        JCommander optionParser = JCommander.newBuilder().addObject(options).build();
        try {
            optionParser.parse(args);
        } catch (ParameterException e) {
            System.err.println("Unable to parse options: " + e.getLocalizedMessage());
            optionParser.usage();
            return;
        }
        if (options.artifacts.isEmpty()) {
            optionParser.usage();
            return;
        }
        if (options.exports_generation.equals(ExportsGenerationType.inherit)) {
            optionParser.usage();
            return;
        }

        final DependencyTools dependencyTools;
        if (options.rule_prefix.isEmpty()) {
            dependencyTools = new DependencyTools();
        } else {
            dependencyTools = new DependencyToolsWithPrefix(options.rule_prefix);
        }

        Merger driver = new Merger(options);
        Collection<ResolutionOutput> resolutionOutputs = driver.readResolutions(options);
        Collection<Resolution> resolutions = resolutionOutputs.stream().map(ResolutionOutput::resolution).collect(Collectors.toList());

        System.out.print("Verifying resolved artifacts graphs...");
        resolutions.forEach(
                resolution -> {
                    GraphVerifications.checkResolutionSuccess(resolution);
                    GraphVerifications.checkAllGraphDependenciesAreResolved(resolution);
                    GraphVerifications.checkGraphDoesNotHaveDanglingDependencies(resolution);
                });
        System.out.println("✓");

        Collection<Dependency> dependencies = driver.mergeResolutions(resolutions);

        System.out.print("Verifying merged graph...");
        GraphVerifications.checkNoRepeatingDependencies(dependencies);
        GraphVerifications.checkAllDependenciesAreResolved(dependencies);
        GraphVerifications.checkNoConflictingVersions(dependencies);
        System.out.println("✓");

        final File artifactsFolder =
                new File(options.artifacts_path.replace("~", System.getProperty("user.home")));
        if (!artifactsFolder.isDirectory() && !artifactsFolder.mkdirs()) {
            throw new IOException(
                    "Failed to create artifacts folder " + artifactsFolder.getAbsolutePath());
        }
        System.out.println("artifactsFolder: " + artifactsFolder.getAbsolutePath());
        final ArtifactDownloader artifactDownloader =
                new ArtifactDownloader(artifactsFolder, dependencyTools);

        final Function<Dependency, URI> downloader =
                dependency1 -> {
                    try {
                        return artifactDownloader.getLocalUriForDependency(dependency1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };

        if (options.fetch_srcjar) {
            System.out.print("Locating sources JARs for resolved dependencies...");
            dependencies = new SourcesJarLocator().fillSourcesAttribute(dependencies);
        } else {
            System.out.print("Clearing srcjar...");
            dependencies = ClearSrcJarAttribute.clearSrcJar(dependencies);
        }
        System.out.println("✓");

        System.out.print("Marking dependencies as test-only...");
        final Predicate<MavenCoordinate> testOnlyDeps = TestOnlyMarker.mark(
                resolutions,
                resolutionOutputs
                        .stream()
                        .filter(ResolutionOutput::testOnly)
                        .map(r -> r.resolution().rootDependency())
                        .collect(Collectors.toSet()));
        System.out.println("✓");

        driver.writeResults(
                resolutionOutputs,
                dependencies,
                testOnlyDeps,
                downloader,
                options,
                dependencyTools);

        if (!options.output_pretty_dep_graph_filename.isEmpty()) {
            File prettyOutput =
                    new File(
                            options.output_target_build_files_base_path,
                            options.output_pretty_dep_graph_filename);
            if (!prettyOutput.getParentFile().isDirectory()
                    && !prettyOutput.getParentFile().mkdirs()) {
                throw new IOException(
                        "Failed to create folder for pretty dependency graph "
                                + prettyOutput.getAbsolutePath());
            }

            try (final OutputStreamWriter fileWriter =
                         new OutputStreamWriter(
                                 new FileOutputStream(prettyOutput, false), Charsets.UTF_8)) {
                final Collection<Dependency> dependenciesToPrint = dependencies;
                List<Resolution> resolutionsToPrint =
                        resolutions.stream()
                                .map(
                                        old ->
                                                Resolution.create(
                                                        old.rootDependency(),
                                                        dependenciesToPrint))
                                .collect(Collectors.toList());

                DfsTraveller(
                        resolutionsToPrint,
                        (dependency, level) -> {
                            try {
                                if (level == 1) {
                                    fileWriter.append(NEW_LINE);
                                    fileWriter.append(" * ");
                                } else {
                                    fileWriter.append("   ");
                                    for (int i = 1; i < level; i++) {
                                        fileWriter.append("   ");
                                    }
                                }

                                fileWriter
                                        .append(dependencyTools.mavenCoordinates(dependency))
                                        .append(" (")
                                        .append(dependencyTools.repositoryRuleName(dependency))
                                        .append(")")
                                        .append(NEW_LINE);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

                System.out.printf(
                        Locale.ROOT,
                        "Stored textual dependencies graph at %s%n",
                        prettyOutput.getAbsolutePath());
            }
        }
    }

    /**
     * By default File#delete fails for non-empty directories, it works like "rm". We need something
     * a little more brutal - this does the equivalent of "rm -r"
     *
     * @param path Root File Path
     * @return true iff the file and all sub files/directories have been removed
     * @throws FileNotFoundException if the path to remove does not exist
     * @implNote taken from https://stackoverflow.com/a/4026761/1324235
     */
    private static boolean deleteRecursive(File path) throws FileNotFoundException {
        if (!path.exists()) {
            throw new FileNotFoundException(path.getAbsolutePath());
        }
        boolean ret = true;
        if (path.isDirectory()) {
            final File[] files = path.listFiles();
            if (files != null) {
                for (File f : files) {
                    ret = ret && deleteRecursive(f);
                }
            }
        }
        return ret && path.delete();
    }

    private Collection<ResolutionOutput> readResolutions(CommandLineOptions options) {
        System.out.printf(Locale.ROOT, "Reading %s root artifacts...", options.artifacts.size());

        final Serialization serialization = new Serialization();
        final List<ResolutionOutput> resolutions =
                options.artifacts.stream()
                        .map(inputFile -> {
                            System.out.print('.');
                            try (final FileReader reader =
                                         new FileReader(inputFile, Charsets.UTF_8)) {
                                return serialization.deserialize(reader);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList());
        System.out.println();

        return resolutions;
    }

    private Collection<Dependency> mergeResolutions(Collection<Resolution> resolutions) {
        System.out.printf(Locale.ROOT, "Merging %s root dependencies...", resolutions.size());
        final Collection<Dependency> merged = merger.mergeGraphs(resolutions);
        System.out.println("✓");
        return merged;
    }

    private void writeResults(
            Collection<ResolutionOutput> resolutions,
            Collection<Dependency> resolvedDependencies,
            final Predicate<MavenCoordinate> testOnlyDeps,
            final Function<Dependency, URI> downloader,
            final CommandLineOptions options,
            DependencyTools dependencyTools)
            throws Exception {
        final Set<MavenCoordinate> rootDependencies = resolutions.stream().map(r -> r.resolution().rootDependency()).collect(Collectors.toSet());

        final Set<MavenCoordinate> exportsForDependency =
                resolutions.stream()
                .map(r -> Pair.of(r.exportsGenerationType(), r.resolution().allResolvedDependencies()))
                .flatMap(p -> p.getRight().stream().map(d -> Pair.of(d.mavenCoordinate(), p.getKey())))
                .map(p -> Pair.of(p.getLeft(), p.getRight().equals(ExportsGenerationType.inherit)? options.exports_generation : p.getRight()))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue, ExportsGenerationType::prioritizeType))
                .entrySet().stream()
                .filter(p -> {
                    switch (p.getValue()) {
                        case all:
                            return true;
                        case requested_deps:
                            return rootDependencies.contains(p.getKey());
                        default:
                            return false;
                    }
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        resolvedDependencies =
                resolvedDependencies.stream()
                        .filter(dependency -> !dependency.url().isEmpty())
                        .map(d -> {
                            if (exportsForDependency.contains(d.mavenCoordinate())) {
                                return Dependency.builder(d).exports(d.dependencies()).build();
                            } else {
                                return Dependency.builder(d).exports(Collections.emptyList()).build();
                            }
                        })
                        .collect(Collectors.toList());

        System.out.println();
        // first, deleting everything that's already there.
        final File depsFolder = actualMacrosFile.getParentFile();
        if (depsFolder.isDirectory() && !options.keep_output_folder) {
            deleteRecursive(depsFolder);
        }

        if (!depsFolder.isDirectory() && !depsFolder.mkdirs()) {
            throw new IOException(
                    "Failed to create folder for dependency files: "
                            + depsFolder.getAbsolutePath());
        }

        try (final OutputStreamWriter fileWriter =
                     new OutputStreamWriter(
                             new FileOutputStream(new File(depsFolder, "BUILD.bazel"), false),
                             Charsets.UTF_8)) {
            fileWriter.append("\"\"\"").append(NEW_LINE);
            fileWriter.append("Auto-generated by https://github.com/menny/mabel").append(NEW_LINE);
            fileWriter.append("\"\"\"").append(NEW_LINE);
        }
        try (final OutputStreamWriter fileWriter =
                     new OutputStreamWriter(new FileOutputStream(macrosFile, false), Charsets.UTF_8)) {
            fileWriter.append("\"\"\"").append(NEW_LINE);
            fileWriter
                    .append(
                            "Required macros for creating repository-rules and third-party Maven dependencies.")
                    .append(NEW_LINE);
            fileWriter.append(NEW_LINE);
            fileWriter.append("Auto-generated by https://github.com/menny/mabel").append(NEW_LINE);
            fileWriter.append("\"\"\"").append(NEW_LINE);
            fileWriter.append(NEW_LINE);
        }

        System.out.printf(Locale.ROOT, "Processing %d targets rules...%n", resolvedDependencies.size());

        final Map<MavenCoordinate, TargetType> targetTypeMap = resolutions
                .stream()
                .collect(Collectors.toMap(r -> r.resolution().rootDependency(), r -> r.targetType().equals(TargetType.inherit) ? options.type : r.targetType()));
        final Function<Dependency, TargetsBuilder> ruleMapper = buildRuleMapper(
                downloader,
                dep -> targetTypeMap.getOrDefault(dep, TargetType.auto /*once we move to jvm_import, this should be changed to naive*/),
                rootDependencies,
                resolvedDependencies);
        final TargetsBuilder fileImporter =
                new TargetsBuilders.HttpTargetsBuilder(options.calculate_sha, downloader);
        ProgressTimer timer =
                new ProgressTimer(
                        resolvedDependencies.size(),
                        "Constructing Bazel targets",
                        "%d out of %d (%.2f%%%s): %s...");
        List<TargetsToWrite> targetsToWritePairs =
                resolvedDependencies.stream()
                        .peek(d -> timer.taskDone(dependencyTools.mavenCoordinates(d)))
                        .map(dependency -> Dependency.builder(dependency)
                                .testOnly(testOnlyDeps.test(dependency.mavenCoordinate()))
                                .build())
                        .map(dependency -> new TargetsToWrite(
                                fileImporter.buildTargets(dependency, dependencyTools),
                                ruleMapper.apply(dependency)
                                        .buildTargets(dependency, dependencyTools)))
                        .collect(Collectors.toList());

        timer.finish();

        System.out.print("Writing targets to files...");
        repositoryRulesMacroWriter.write(
                targetsToWritePairs.stream()
                        .map(t -> t.repositoryRules)
                        .flatMap(List::stream)
                        .collect(Collectors.toList()));

        final Function<Target, Target> visibilityFixer =
                PublicTargetsCategory.create(
                        options.public_targets_category,
                        rootDependencies,
                        resolvedDependencies);

        List<Target> targets =
                targetsToWritePairs.stream()
                        .map(t -> t.targets)
                        .flatMap(List::stream)
                        .map(visibilityFixer)
                        .collect(Collectors.toList());
        hardAliasesWriter.write(targets);
        targetsMacroWriter.write(targets);

        Files.move(macrosFile.toPath(), actualMacrosFile.toPath(), REPLACE_EXISTING);

        System.out.println("✓");
    }

    private Function<Dependency, TargetsBuilder> buildRuleMapper(
            final Function<Dependency, URI> downloader,
            final Function<MavenCoordinate, TargetType> targetTypeProvider,
            final Set<MavenCoordinate> rootCoordinates,
            final Collection<Dependency> resolvedDependencies) {
        TargetsBuilderForType typer = new TargetsBuilderForType(targetTypeProvider, downloader);
        TargetCommenter commenter = new TargetCommenter(rootCoordinates, resolvedDependencies);
        return dependency -> commenter.createTargetBuilder(typer.generateBuilder(dependency));

    }

    private static class TargetsToWrite {
        final List<Target> repositoryRules;
        final List<Target> targets;

        private TargetsToWrite(List<Target> repositoryRules, List<Target> targets) {
            this.repositoryRules = repositoryRules;
            this.targets = targets;
        }
    }
}
