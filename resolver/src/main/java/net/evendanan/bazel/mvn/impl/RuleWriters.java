package net.evendanan.bazel.mvn.impl;

import static net.evendanan.bazel.mvn.impl.RuleFormatters.HTTP_FILE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.devtools.bazel.workspace.maven.Rule;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Logger;
import net.evendanan.bazel.mvn.api.RuleFormatter;
import net.evendanan.bazel.mvn.api.RuleWriter;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.timing.TaskTiming;
import net.evendanan.timing.TimingData;

public class RuleWriters {

    private final static Logger logger = Logger.getLogger(
        MethodHandles.lookup().lookupClass().getName());

    private static final String INDENT = "    ";
    private static final String NEW_LINE = System.lineSeparator();

    public static class HttpRepoRulesMacroWriter implements RuleWriter {

        private final File outputFile;
        private final String macroName;

        public HttpRepoRulesMacroWriter(final File outputFile, final String macroName) {
            this.outputFile = outputFile;
            this.macroName = macroName;
        }

        @Override
        public void write(final Collection<Rule> rules) throws IOException {
            logger.info(String.format("Writing %d Bazel repository rules...", rules.size()));

            try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(outputFile, true), Charsets.UTF_8)) {
                fileWriter.append("# Loading a drop-in replacement for native.http_file").append(NEW_LINE);
                fileWriter.append("load('@bazel_tools//tools/build_defs/repo:http.bzl', 'http_file')").append(NEW_LINE);
                fileWriter.append(NEW_LINE);

                fileWriter.append("# Repository rules macro to be run in the WORKSPACE file.").append(NEW_LINE);
                fileWriter.append("def ").append(macroName).append("():").append(NEW_LINE);
                if (rules.isEmpty()) {
                    fileWriter.append(INDENT).append("pass");
                } else {
                    for (Rule rule : rules) {
                        fileWriter.append(HTTP_FILE.formatRule(INDENT, rule));
                    }
                }
                fileWriter.append(NEW_LINE);
            }
        }
    }

    public static class TransitiveRulesMacroWriter implements RuleWriter {

        private final File outputFile;
        private final String macroName;
        private final Function<Rule, RuleFormatter> formatterMapper;

        public TransitiveRulesMacroWriter(final File outputFile, final String macroName, Function<Rule, RuleFormatter> formatterMapper) {
            this.outputFile = outputFile;
            this.macroName = macroName;
            this.formatterMapper = formatterMapper;
        }

        @Override
        public void write(final Collection<Rule> rules) throws IOException {
            try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(outputFile, true), Charsets.UTF_8)) {
                fileWriter.append("# Transitive rules macro to be run in the BUILD.bazel file.").append(NEW_LINE);
                fileWriter.append("# If you use kt_* rules, you MUST provide the correct rule implementation when call this macro, if you decide").append(NEW_LINE);
                fileWriter.append("# not to provide those implementations we'll try to use java_* rules.").append(NEW_LINE);
                fileWriter.append(NEW_LINE);

                fileWriter.append("def ").append(macroName).append("(kt_jvm_import=None, kt_jvm_library=None):").append(NEW_LINE);
                if (rules.isEmpty()) {
                    fileWriter.append(INDENT).append("pass");
                } else {
                    final TaskTiming timer = new TaskTiming();
                    logger.info(String.format("Writing %d Bazel rules...", rules.size()));
                    timer.start();
                    timer.setTotalTasks(rules.size());

                    for (Rule rule : rules) {
                        final TimingData data = timer.taskDone();
                        final String estimatedTimeLeft;
                        if (data.doneTasks >= 10) {
                            estimatedTimeLeft = String.format(Locale.US, ", %s left", TaskTiming.humanReadableTime(data.estimatedTimeLeft));
                        } else {
                            estimatedTimeLeft = "";
                        }
                        System.out.println(
                            String.format(Locale.US, "** Writing rule %d out of %d (%.2f%%%s): %s...",
                                data.doneTasks, data.totalTasks, 100 * data.ratioOfDone, estimatedTimeLeft,
                                rule.mavenGeneratedName()));
                        fileWriter.append(formatterMapper.apply(rule).formatRule(INDENT, rule)).append(NEW_LINE);
                    }
                }
                fileWriter.append(NEW_LINE);
            }
        }
    }

    public static class TransitiveRulesAliasWriter implements RuleWriter {

        private final File baseFolder;
        private final String pathToTransitiveRulesPackage;

        public TransitiveRulesAliasWriter(final File baseFolder, final String pathToTransitiveRulesPackage) {
            this.baseFolder = baseFolder;
            this.pathToTransitiveRulesPackage = pathToTransitiveRulesPackage;
        }

        @Override
        public void write(final Collection<Rule> rules) throws IOException {
            System.out.println("Will write " + rules.size() + " hard aliases files to base folder " + baseFolder + "...");
            for (final Rule rule : rules) {
                final File buildFileFolder = new File(baseFolder, getFilePathFromMavenName(rule.groupId(), rule.artifactId()));
                if (!buildFileFolder.exists() && !buildFileFolder.mkdirs()) {
                    throw new IOException("Failed to create folder " + buildFileFolder.getAbsolutePath());
                }
                try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(new File(buildFileFolder, "BUILD.bazel"), false), Charsets.UTF_8)) {
                    fileWriter.append("#Auto-generated by https://github.com/menny/bazel-mvn-deps").append(NEW_LINE);
                    fileWriter.append(new Target("alias", buildFileFolder.getName())
                        .addString("actual", String.format(Locale.US, "//%s:%s", pathToTransitiveRulesPackage, rule.safeRuleFriendlyName()))
                        .setPublicVisibility()
                        .outputString(""))
                        .append(NEW_LINE);
                }
            }
        }
    }

    @VisibleForTesting
    static String getFilePathFromMavenName(String group, String artifactId) {
        return String.format(Locale.US, "%s/%s/",
            group.replaceAll("\\.", "/"), artifactId);
    }
}
