package net.evendanan.bazel.mvn;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.IParameterSplitter;
import com.google.common.base.Charsets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.GraphMerger;
import net.evendanan.bazel.mvn.api.RuleWriter;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.impl.RuleClassifiers;
import net.evendanan.bazel.mvn.impl.RuleWriters;
import net.evendanan.bazel.mvn.impl.TargetsBuilders;
import net.evendanan.bazel.mvn.merger.ClearSrcJarAttribute;
import net.evendanan.bazel.mvn.merger.DefaultMerger;
import net.evendanan.bazel.mvn.merger.DependencyNamePrefixer;
import net.evendanan.bazel.mvn.merger.DependencyTreeFlatter;
import net.evendanan.bazel.mvn.merger.SourcesJarLocator;
import net.evendanan.bazel.mvn.serialization.Serialization;
import net.evendanan.timing.TaskTiming;
import net.evendanan.timing.TimingData;

public class Merger {

    private final GraphMerger merger;
    private final RuleWriter repositoryRulesMacroWriter;
    private final RuleWriter targetsMacroWriter;
    private final RuleWriter hardAliasesWriter;
    private final File macrosFile;

    private Merger(final Options options) {
        final File parent = new File(options.output_target_build_files_base_path);
        this.macrosFile = new File(parent, options.output_macro_file);
        this.merger = new DefaultMerger();
        this.repositoryRulesMacroWriter = new RuleWriters.HttpRepoRulesMacroWriter(
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

        Merger driver = new Merger(options);
        Collection<Dependency> dependencies = driver.generateFromInputs(options);

        if (options.fetch_srcjar) {
            System.out.print("Locating sources JARs for resolved dependencies...");
            dependencies = new SourcesJarLocator().fillSourcesAttribute(dependencies);
            System.out.println("✓");
        } else {
            System.out.print("Clearing srcjar...");
            dependencies = ClearSrcJarAttribute.clearSrcJar(dependencies);
            System.out.println();
        }

        if (!options.rule_prefix.isEmpty()) {
            dependencies = DependencyNamePrefixer.wrap(dependencies, options.rule_prefix);
        }

        driver.writeResults(dependencies, args);
    }

    /**
     * By default File#delete fails for non-empty directories, it works like "rm".
     * We need something a little more brutal - this does the equivalent of "rm -r"
     *
     * @param path Root File Path
     *
     * @return true iff the file and all sub files/directories have been removed
     *
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
            if (files!=null) {
                for (File f : files) {
                    ret = ret && deleteRecursive(f);
                }
            }
        }
        return ret && path.delete();
    }

    private Collection<Dependency> generateFromInputs(Options options) {
        System.out.print(String.format("Reading %s root artifacts...", options.artifacts.size()));

        final Serialization serialization = new Serialization();
        final List<Dependency> dependencies = options.artifacts.stream()
                .map(inputFile -> {
                    System.out.print('.');
                    try (final InputStreamReader fileReader = new InputStreamReader(new FileInputStream(inputFile), Charsets.UTF_8)) {
                        return serialization.deserialize(fileReader);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        System.out.println();

        System.out.print(String.format("Merging %s dependencies...", dependencies.size()));
        final Collection<Dependency> merged = merger.mergeGraphs(dependencies);
        System.out.println();
        return merged;
    }

    private void writeResults(Collection<Dependency> resolvedDependencies, final String[] args) throws Exception {
        System.out.print("Flattening dependency tree for writing...");
        resolvedDependencies = DependencyTreeFlatter.flatten(resolvedDependencies)
                .stream()
                .filter(dependency -> !dependency.url().toASCIIString().equals(""))
                .collect(Collectors.toList());

        System.out.println();
        //first, deleting everything that's already there.
        final File depsFolder = macrosFile.getParentFile();
        if (depsFolder.isDirectory()) {
            deleteRecursive(depsFolder);
        }

        if (!depsFolder.mkdirs()) {
            throw new IOException("Failed to create folder for dependency files: " + depsFolder.getAbsolutePath());
        }

        try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(new File(depsFolder, "BUILD.bazel"), false), Charsets.UTF_8)) {
            fileWriter.append("# Auto-generated by https://github.com/menny/bazel-mvn-deps").append(System.lineSeparator());
            fileWriter.append(System.lineSeparator());
            fileWriter.append("# Args: ").append(String.join(" ", args)).append(System.lineSeparator());
        }
        try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(macrosFile, false), Charsets.UTF_8)) {
            fileWriter.append("# Auto-generated by https://github.com/menny/bazel-mvn-deps").append(System.lineSeparator());
            fileWriter.append(System.lineSeparator());
            fileWriter.append(System.lineSeparator());
        }

        final TaskTiming timer = new TaskTiming();
        System.out.println(String.format("Processing %s resolved rules...", resolvedDependencies.size()));

        timer.start(resolvedDependencies.size());
        List<Target> targets = resolvedDependencies.stream()
                .peek(dependency -> {
                    final TimingData timingData = timer.taskDone();
                    final String estimatedTimeLeft;
                    if (timingData.doneTasks >= 3) {
                        estimatedTimeLeft = String.format(Locale.ROOT, ", %s left", TaskTiming.humanReadableTime(timingData.estimatedTimeLeft));
                    } else {
                        estimatedTimeLeft = "";
                    }
                    System.out.println(
                            String.format(Locale.ROOT, "** Converting to Bazel targets, %d out of %d (%.2f%%%s): %s...",
                                    timingData.doneTasks, timingData.totalTasks, 100 * timingData.ratioOfDone, estimatedTimeLeft,
                                    dependency.repositoryRuleName()));
                })
                .map(rule -> RuleClassifiers.NATIVE_RULE_MAPPER.apply(rule).buildTargets(rule))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        System.out.print(String.format("Writing %d Bazel repository rules...", targets.size()));
        repositoryRulesMacroWriter.write(resolvedDependencies.stream()
                .map(TargetsBuilders.HTTP_FILE::buildTargets)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));
        System.out.println("✓");

        System.out.print(String.format("Writing %d Bazel dependency targets...", targets.size()));
        targetsMacroWriter.write(targets);
        System.out.println("✓");

        if (hardAliasesWriter!=null) {
            System.out.print("Writing aliases targets...");
            hardAliasesWriter.write(targets);
            System.out.println("✓");
        }
    }

    @Parameters(separators = "=")
    public static class Options {

        @Parameter(
                names = {"--graph_file", "-a"},
                splitter = NoSplitter.class,
                description = "JSON files representing dependency-graph.",
                required = true
        ) List<String> artifacts = new ArrayList<>();

        @Parameter(
                names = {"--output_macro_file_path"},
                description = "Path to output macros bzl file",
                required = true
        ) String output_macro_file = "";

        @Parameter(
                names = {"--output_target_build_files_base_path"},
                description = "Base path to output alias targets BUILD.bazel files"
        ) String output_target_build_files_base_path = "";

        @Parameter(
                names = {"--package_path"},
                description = "Package path for for transitive rules."
        ) String package_path = "";

        @Parameter(
                names = {"--rule_prefix"},
                description = "Prefix to add to all rules."
        ) String rule_prefix = "";

        @Parameter(
                names = {"--create_deps_sub_folders"},
                description = "Generate sub-folders matching dependencies tree.",
                arity = 1
        ) boolean create_deps_sub_folders = true;

        @Parameter(
                names = {"--fetch_srcjar"},
                description = "Will also try to locate srcjar for the dependency.",
                arity = 1
        ) boolean fetch_srcjar = true;

        @Parameter(
                names = {"--debug_logs"},
                description = "Will print out debug logs.",
                arity = 1
        ) boolean debug_logs = false;
    }

    /**
     * Jcommander defaults to splitting each parameter by comma. For example,
     * --a=group:artifact:[x1,x2] is parsed as two items 'group:artifact:[x1' and 'x2]',
     * instead of the intended 'group:artifact:[x1,x2]'
     *
     * For more information: http://jcommander.org/#_splitting
     */
    public static class NoSplitter implements IParameterSplitter {

        @Override
        public List<String> split(String value) {
            return Collections.singletonList(value);
        }
    }
}
