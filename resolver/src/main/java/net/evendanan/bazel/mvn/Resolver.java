package net.evendanan.bazel.mvn;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.IParameterSplitter;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.devtools.bazel.workspace.maven.DefaultModelResolver;
import com.google.devtools.bazel.workspace.maven.GraphResolver;
import com.google.devtools.bazel.workspace.maven.Rule;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.RuleWriter;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.impl.RuleClassifiers;
import net.evendanan.bazel.mvn.impl.RuleWriters;
import net.evendanan.bazel.mvn.impl.TargetsBuilders;
import net.evendanan.timing.TaskTiming;
import net.evendanan.timing.TimingData;
import org.apache.maven.model.Repository;

public class Resolver {

    private final static Logger logger = Logger.getLogger("GraphResolver");

    private final GraphResolver resolver;
    private final RuleWriter repositoryRulesMacroWriter;
    private final RuleWriter targetsMacroWriter;
    private final RuleWriter hardAliasesWriter;
    private final File macrosFile;

    private Resolver(final Options options) {
        final File parent = new File(options.output_target_build_files_base_path);
        this.macrosFile = new File(parent, options.output_macro_file);
        this.resolver = new GraphResolver(new DefaultModelResolver(buildRepositories(options.repositories)), options.blacklist, options.rule_prefix);
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
        if (options.repositories.isEmpty()) {
            optionParser.usage();
            return;
        }

        Resolver driver = new Resolver(options);
        driver.generateFromArtifacts(options.artifacts);
        driver.writeResults(args);
    }

    private static List<Repository> buildRepositories(List<String> repositories) {
        ArrayList<Repository> repositoryList = new ArrayList<>(repositories.size());
        for (String repositoryUrlString : repositories) {
            Preconditions.checkState(repositoryUrlString.endsWith("/"), "Repository url '%s' should end with '/'", repositoryUrlString);
            final Repository repository = new Repository();
            URI repositoryUri = URI.create(repositoryUrlString);
            repository.setId(repositoryUri.getHost());
            repository.setName(repositoryUri.getHost());
            repository.setUrl(repositoryUrlString);
            repositoryList.add(repository);
        }

        return repositoryList;
    }

    /**
     * By default File#delete fails for non-empty directories, it works like "rm".
     * We need something a little more brutual - this does the equivalent of "rm -r"
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

    private void generateFromArtifacts(List<String> artifacts) {
        final TaskTiming timer = new TaskTiming();
        List<Rule> rules = new ArrayList<>();
        logger.info(String.format("Processing %s root artifacts...", artifacts.size()));

        for (final String artifact : artifacts) {
            resolver.createRule(artifact).ifPresent(rules::add);
        }

        timer.start(artifacts.size());
        for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
            final Rule rule = rules.get(ruleIndex);
            final TimingData timingData = timer.taskDone();
            final String estimatedTimeLeft;
            if (ruleIndex >= 3) {
                estimatedTimeLeft = String.format(Locale.US, ", %s left", TaskTiming.humanReadableTime(timingData.estimatedTimeLeft));
            } else {
                estimatedTimeLeft = "";
            }
            System.out.println(
                    String.format(Locale.US, "** Resolving dependency graph for artifact %d out of %d (%.2f%%%s): %s...",
                            timingData.doneTasks, timingData.totalTasks, 100 * timingData.ratioOfDone, estimatedTimeLeft,
                            rule.mavenGeneratedName()));
            resolver.resolveRuleArtifacts(rule);
        }
    }

    private void writeResults(final String[] args) throws Exception {
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
        final Collection<Rule> resolvedRules = resolver.getRules();
        logger.info(String.format("Processing %s resolved rules...", resolvedRules.size()));

        timer.start(resolvedRules.size());
        List<Target> targets = resolvedRules.stream()
                .peek(rule -> {
                    final TimingData timingData = timer.taskDone();
                    final String estimatedTimeLeft;
                    if (timingData.doneTasks >= 3) {
                        estimatedTimeLeft = String.format(Locale.US, ", %s left", TaskTiming.humanReadableTime(timingData.estimatedTimeLeft));
                    } else {
                        estimatedTimeLeft = "";
                    }
                    System.out.println(
                            String.format(Locale.US, "** Converting to Bazel targets, %d out of %d (%.2f%%%s): %s...",
                                    timingData.doneTasks, timingData.totalTasks, 100 * timingData.ratioOfDone, estimatedTimeLeft,
                                    rule.mavenGeneratedName()));
                })
                .map(rule -> RuleClassifiers.NATIVE_RULE_MAPPER.apply(rule).buildTargets(rule))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        repositoryRulesMacroWriter.write(resolvedRules.stream()
                .map(TargetsBuilders.HTTP_FILE::buildTargets)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));

        targetsMacroWriter.write(targets);

        if (hardAliasesWriter!=null) {
            logger.info("Writing aliases targets...");
            hardAliasesWriter.write(targets);
        }
    }

    @Parameters(separators = "=")
    public static class Options {

        @Parameter(
                names = {"--artifact", "-a"},
                splitter = NoSplitter.class,
                description = "Maven artifact coordinates (e.g. groupId:artifactId:version).",
                required = true
        ) List<String> artifacts = new ArrayList<>();

        @Parameter(
                names = {"--blacklist", "-b"},
                splitter = NoSplitter.class,
                description = "Blacklisted Maven artifact coordinates (e.g. groupId:artifactId:version)."
        ) List<String> blacklist = new ArrayList<>();

        @Parameter(
                names = {"--repository"},
                splitter = NoSplitter.class,
                description = "Maven repository url.",
                required = true
        ) List<String> repositories = new ArrayList<>();

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