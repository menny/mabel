package net.evendanan.bazel.mvn;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.IParameterSplitter;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import net.evendanan.bazel.mvn.api.RuleWriter;
import net.evendanan.bazel.mvn.impl.RuleClassifiers;
import net.evendanan.bazel.mvn.impl.RuleWriters;
import net.evendanan.timing.TaskTiming;
import net.evendanan.timing.TimingData;
import org.apache.maven.model.Repository;

public class Resolver {

    private final static Logger logger = Logger.getLogger("GraphResolver");

    private final GraphResolver resolver;
    private final RuleWriter repositoryRulesWriter;
    private final List<RuleWriter> targetRulesWriters;
    private final File macrosFile;

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

    private Resolver(final Options options) {
        macrosFile = new File(options.output_macro_file);
        this.resolver = new GraphResolver(new DefaultModelResolver(buildRepositories(options.repositories)), options.blacklist);
        this.repositoryRulesWriter = new RuleWriters.HttpRepoRulesMacroWriter(
            macrosFile,
            "generate_workspace_rules");
        ArrayList<RuleWriter> writers = new ArrayList<>(2);
        writers.add(new RuleWriters.TransitiveRulesMacroWriter(
            macrosFile,
            "generate_transitive_dependency_targets",
            RuleClassifiers.NATIVE_RULE_MAPPER));
        if (!options.output_target_build_files_base_path.isEmpty()) {
            if (options.package_path.isEmpty()) {
                throw new IllegalArgumentException("--package_path can not be empty, if --output_target_build_files_base_path was set.");
            }
            writers.add(new RuleWriters.TransitiveRulesAliasWriter(
                new File(options.output_target_build_files_base_path),
                options.package_path));
        }
        this.targetRulesWriters = ImmutableList.copyOf(writers);
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

    private void generateFromArtifacts(List<String> artifacts) {
        final TaskTiming timer = new TaskTiming();
        List<Rule> rules = new ArrayList<>();
        logger.info(String.format("Processing %s root artifacts...", artifacts.size()));

        for (final String artifact : artifacts) {
            resolver.createRule(artifact).ifPresent(rules::add);
        }

        timer.start();
        timer.setTotalTasks(artifacts.size());
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
                String.format(Locale.US, "** Processing rule %d out of %d (%.2f%%%s): %s...",
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
            fileWriter.append("# Args: ").append(String.join(" ", args)).append(System.lineSeparator());
            fileWriter.append(System.lineSeparator());
            fileWriter.append(System.lineSeparator());
        }
        repositoryRulesWriter.write(resolver.getRules());
        for (RuleWriter writer : targetRulesWriters) {
            writer.write(resolver.getRules());
        }
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
            if (files != null) {
                for (File f : files) {
                    ret = ret && deleteRecursive(f);
                }
            }
        }
        return ret && path.delete();
    }

    @Parameters(separators = "=")
    public static class Options {

        @Parameter(
            names = { "--artifact", "-a" },
            splitter = NoSplitter.class,
            description = "Maven artifact coordinates (e.g. groupId:artifactId:version).",
            required = true
        ) List<String> artifacts = new ArrayList<>();

        @Parameter(
            names = { "--blacklist", "-b" },
            splitter = NoSplitter.class,
            description = "Blacklisted Maven artifact coordinates (e.g. groupId:artifactId:version)."
        ) List<String> blacklist = new ArrayList<>();

        @Parameter(
            names = { "--repository" },
            splitter = NoSplitter.class,
            description = "Maven repository url.",
            required = true
        ) List<String> repositories = new ArrayList<>();

        @Parameter(
            names = { "--output_macro_file_path" },
            description = "Path to output macros bzl file",
            required = true
        ) String output_macro_file = "";

        @Parameter(
            names = { "--output_target_build_files_base_path" },
            description = "Base path to output alias targets BUILD.bazel files"
        ) String output_target_build_files_base_path = "";

        @Parameter(
            names = { "--package_path" },
            description = "Package path for for transitive rules."
        ) String package_path = "";

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
