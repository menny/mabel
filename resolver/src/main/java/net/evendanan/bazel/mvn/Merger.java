package net.evendanan.bazel.mvn;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.IParameterSplitter;
import com.google.common.base.Charsets;
import net.evendanan.bazel.mvn.api.*;
import net.evendanan.bazel.mvn.impl.RuleClassifiers;
import net.evendanan.bazel.mvn.impl.RuleWriters;
import net.evendanan.bazel.mvn.impl.TargetsBuilders;
import net.evendanan.bazel.mvn.merger.*;
import net.evendanan.timing.TaskTiming;
import net.evendanan.timing.TimingData;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static net.evendanan.bazel.mvn.merger.GraphUtils.DfsTraveller;

public class Merger {

    private final GraphMerger merger;
    private final RuleWriter repositoryRulesMacroWriter;
    private final RuleWriter targetsMacroWriter;
    private final RuleWriter hardAliasesWriter;
    private final File macrosFile;
    private final File actualMacrosFile;

    private Merger(final Options options) throws IOException {
        final File parent = new File(options.output_target_build_files_base_path);
        actualMacrosFile = new File(parent, options.output_macro_file);
        macrosFile = File.createTempFile("temp_mabel_deps", actualMacrosFile.getName());

        merger = new DefaultMerger();
        repositoryRulesMacroWriter = new RuleWriters.HttpRepoRulesMacroWriter(
                macrosFile,
                "generate_workspace_rules");
        targetsMacroWriter = new RuleWriters.TransitiveRulesMacroWriter(
                macrosFile,
                "generate_transitive_dependency_targets");

        if (options.create_deps_sub_folders) {
            if (options.package_path.isEmpty()) {
                throw new IllegalArgumentException("--package_path can not be empty, if --output_target_build_files_base_path was set.");
            }
            hardAliasesWriter = new RuleWriters.TransitiveRulesAliasWriter(parent, options.package_path);
        } else {
            hardAliasesWriter = null;
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = new Options();
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

        final DependencyTools dependencyTools;
        if (options.rule_prefix.isEmpty()) {
            dependencyTools = new DependencyTools();
        } else {
            dependencyTools = new DependencyToolsWithPrefix(options.rule_prefix);
        }

        Merger driver = new Merger(options);
        Collection<Resolution> resolutions = driver.readResolutions(options);

        System.out.print("Verifying resolved artifacts graphs...");
        resolutions.forEach(resolution -> {
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

        final File artifactsFolder = new File(options.artifacts_path.replace("~", System.getProperty("user.home")));
        if (!artifactsFolder.isDirectory() && !artifactsFolder.mkdirs()) {
            throw new IOException("Failed to create artifacts folder " + artifactsFolder.getAbsolutePath());
        }
        System.out.println("artifactsFolder: " + artifactsFolder.getAbsolutePath());
        final ArtifactDownloader artifactDownloader = new ArtifactDownloader(artifactsFolder, dependencyTools);

        final Function<Dependency, URI> downloader = dependency1 -> {
            try {
                return artifactDownloader.getLocalUriForDependency(dependency1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        if (options.fetch_srcjar) {
            System.out.print("Locating sources JARs for resolved dependencies...");
            dependencies = new SourcesJarLocator().fillSourcesAttribute(dependencies);
            System.out.println("✓");
        } else {
            System.out.print("Clearing srcjar...");
            dependencies = ClearSrcJarAttribute.clearSrcJar(dependencies);
            System.out.println("✓");
        }

        driver.writeResults(dependencies, downloader, options, dependencyTools);

        if (!options.output_pretty_dep_graph_filename.isEmpty()) {
            File prettyOutput = new File(options.output_target_build_files_base_path, options.output_pretty_dep_graph_filename);
            if (!prettyOutput.getParentFile().isDirectory() && !prettyOutput.getParentFile().mkdirs()) {
                throw new IOException("Failed to create folder for pretty dependency graph " + prettyOutput.getAbsolutePath());
            }

            try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(prettyOutput, false), Charsets.UTF_8)) {
                final Collection<Dependency> dependenciesToPrint = dependencies;
                List<Resolution> resolutionsToPrint = resolutions.stream()
                        .map(Resolution::newBuilder)
                        .map(Resolution.Builder::clearAllResolvedDependencies)
                        .map(builder -> builder.addAllAllResolvedDependencies(dependenciesToPrint))
                        .map(Resolution.Builder::build)
                        .collect(Collectors.toList());

                DfsTraveller(resolutionsToPrint,
                        (dependency, level) -> {
                            try {
                                if (level == 1) {
                                    fileWriter.append(System.lineSeparator());
                                    fileWriter.append(" * ");
                                } else {
                                    fileWriter.append("   ");
                                    for (int i = 1; i < level; i++) {
                                        fileWriter.append("   ");
                                    }
                                }

                                fileWriter.append(dependencyTools.mavenCoordinates(dependency))
                                        .append(" (").append(dependencyTools.repositoryRuleName(dependency)).append(")")
                                        .append(System.lineSeparator());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

                System.out.println(String.format(Locale.ROOT, "Stored textual dependencies graph at %s", prettyOutput.getAbsolutePath()));
            }
        }
    }

    /**
     * By default File#delete fails for non-empty directories, it works like "rm".
     * We need something a little more brutal - this does the equivalent of "rm -r"
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

    private Collection<Resolution> readResolutions(Options options) {
        System.out.print(String.format("Reading %s root artifacts...", options.artifacts.size()));

        final List<Resolution> resolutions = options.artifacts.stream()
                .map(inputFile -> {
                    System.out.print('.');
                    try (final FileInputStream inputStream = new FileInputStream(inputFile)) {
                        return Resolution.parseFrom(inputStream);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        System.out.println();

        return resolutions;
    }

    private Collection<Dependency> mergeResolutions(Collection<Resolution> resolutions) {
        System.out.print(String.format("Merging %s root dependencies...", resolutions.size()));
        final Collection<Dependency> merged = merger.mergeGraphs(resolutions);
        System.out.println("✓");
        return merged;
    }

    private void writeResults(Collection<Dependency> resolvedDependencies, final Function<Dependency, URI> downloader, final Options options, DependencyTools dependencyTools) throws Exception {
        resolvedDependencies = resolvedDependencies
                .stream()
                .filter(dependency -> !dependency.getUrl().equals(""))
                .collect(Collectors.toList());

        System.out.println();
        //first, deleting everything that's already there.
        final File depsFolder = actualMacrosFile.getParentFile();
        if (depsFolder.isDirectory()) {
            deleteRecursive(depsFolder);
        }

        if (!depsFolder.mkdirs()) {
            throw new IOException("Failed to create folder for dependency files: " + depsFolder.getAbsolutePath());
        }

        try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(new File(depsFolder, "BUILD.bazel"), false), Charsets.UTF_8)) {
            fileWriter.append("# Auto-generated by https://github.com/menny/mabel").append(System.lineSeparator());
        }
        try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(macrosFile, false), Charsets.UTF_8)) {
            fileWriter.append("# Auto-generated by https://github.com/menny/mabel").append(System.lineSeparator());
            fileWriter.append(System.lineSeparator());
        }

        System.out.println(String.format("Processing %s targets rules...", resolvedDependencies.size()));

        final Function<Dependency, TargetsBuilder> ruleMapper = buildRuleMapper(downloader);
        final TargetsBuilder fileImporter = new TargetsBuilders.HttpTargetsBuilder(options.calculate_sha, downloader);
        ProgressTimer timer = new ProgressTimer(resolvedDependencies.size(), "Constructing Bazel targets", "%d out of %d (%.2f%%%s): %s...");
        List<TargetsToWrite> targetsToWritePairs = resolvedDependencies.stream()
                .peek(timer::taskDone)
                .map(dependency -> new TargetsToWrite(
                        fileImporter.buildTargets(dependency, dependencyTools),
                        ruleMapper.apply(dependency).buildTargets(dependency, dependencyTools)))
                .collect(Collectors.toList());

        timer.finish();

        System.out.print("Writing targets to files...");
        repositoryRulesMacroWriter.write(targetsToWritePairs.stream()
                .map(t -> t.repositoryRules)
                .flatMap(List::stream)
                .collect(Collectors.toList()));

        List<Target> targets = targetsToWritePairs.stream()
                .map(t -> t.targets)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        if (hardAliasesWriter != null) {
            hardAliasesWriter.write(targets);
        }
        targetsMacroWriter.write(targets);

        Files.move(macrosFile.toPath(), actualMacrosFile.toPath(), REPLACE_EXISTING);

        System.out.println("✓");
    }

    private static class TargetsToWrite {
        final List<Target> repositoryRules;
        final List<Target> targets;

        private TargetsToWrite(List<Target> repositoryRules, List<Target> targets) {
            this.repositoryRules = repositoryRules;
            this.targets = targets;
        }
    }

    private Function<Dependency, TargetsBuilder> buildRuleMapper(final Function<Dependency, URI> downloader) {
        return dependency -> RuleClassifiers.priorityRuleClassifier(
                Arrays.asList(
                        new RuleClassifiers.PomClassifier(),
                        new RuleClassifiers.AarClassifier(),
                        new RuleClassifiers.JarInspector(downloader)),
                TargetsBuilders.JAVA_IMPORT, dependency);
    }

    private static class ProgressTimer {
        private final TaskTiming timer = new TaskTiming();
        private final String title;
        private final String progressText;

        ProgressTimer(int tasksCount, String title, String progressText) {
            this.title = title;
            this.progressText = progressText;
            this.timer.start(tasksCount);
        }

        void taskDone(Dependency dependency) {
            final TimingData timingData = timer.taskDone();
            final String estimatedTimeLeft;
            if (timingData.doneTasks >= 3) {
                estimatedTimeLeft = String.format(Locale.ROOT, ", %s left", TaskTiming.humanReadableTime(timingData.estimatedTimeLeft));
            } else {
                estimatedTimeLeft = "";
            }
            report(progressText,
                    timingData.doneTasks, timingData.totalTasks, 100 * timingData.ratioOfDone, estimatedTimeLeft,
                    DependencyTools.DEFAULT.mavenCoordinates(dependency));
        }

        void finish() {
            TimingData finish = timer.finish();
            report("Finished. %s", TaskTiming.humanReadableTime(finish.totalTime - finish.startTime));
        }

        private void report(String text, Object... args) {
            String msg = String.format(Locale.ROOT, text, args);
            System.out.println(String.format(Locale.ROOT, "[%s] %s", title, msg));
        }
    }

    @Parameters(separators = "=")
    public static class Options {

        @Parameter(
                names = {"--graph_file", "-a"},
                splitter = NoSplitter.class,
                description = "JSON files representing dependency-graph.",
                required = true
        )
        List<String> artifacts = new ArrayList<>();

        @Parameter(
                names = {"--output_macro_file_path"},
                description = "Path to output macros bzl file",
                required = true
        )
        String output_macro_file = "";

        @Parameter(
                names = {"--output_target_build_files_base_path"},
                description = "Base path to output alias targets BUILD.bazel files"
        )
        String output_target_build_files_base_path = "";

        @Parameter(
                names = {"--package_path"},
                description = "Package path for for transitive rules."
        )
        String package_path = "";

        @Parameter(
                names = {"--rule_prefix"},
                description = "Prefix to add to all rules."
        )
        String rule_prefix = "";

        @Parameter(
                names = {"--create_deps_sub_folders"},
                description = "Generate sub-folders matching dependencies tree.",
                arity = 1
        )
        boolean create_deps_sub_folders = true;

        @Parameter(
                names = {"--fetch_srcjar"},
                description = "Will also try to locate srcjar for the dependency.",
                arity = 1
        )
        boolean fetch_srcjar = false;

        @Parameter(
                names = {"--calculate_sha"},
                description = "Will also calculate SHA256 for the dependency.",
                arity = 1
        )
        boolean calculate_sha = true;

        @Parameter(
                names = {"--debug_logs"},
                description = "Will print out debug logs.",
                arity = 1
        )
        boolean debug_logs = false;

        @Parameter(
                names = {"--output_pretty_dep_graph_filename"},
                description = "If set, will output the dependency graph to this file."
        )
        String output_pretty_dep_graph_filename = "";
        @Parameter(
                names = {"--artifacts_path"},
                description = "Where to store downloaded artifacts.",
                required = true
        )
        private String artifacts_path;
    }

    /**
     * Jcommander defaults to splitting each parameter by comma. For example,
     * --a=group:artifact:[x1,x2] is parsed as two items 'group:artifact:[x1' and 'x2]',
     * instead of the intended 'group:artifact:[x1,x2]'
     * <p>
     * For more information: http://jcommander.org/#_splitting
     */
    public static class NoSplitter implements IParameterSplitter {

        @Override
        public List<String> split(String value) {
            return Collections.singletonList(value);
        }
    }
}
