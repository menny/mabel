package net.evendanan.bazel.mvn.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import net.evendanan.bazel.mvn.api.RuleWriter;
import net.evendanan.bazel.mvn.api.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class RuleWriters {

    private static StringBuilder readTemplate(String templateResourceName) throws IOException {
        return readTemplate(templateResourceName, Collections.emptyMap());
    }

    private static StringBuilder readTemplate(String templateResourceName, Map<String, String> replacements) throws IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        Resources.readLines(Resources.getResource(templateResourceName), Charsets.UTF_8)
                .forEach(line -> stringBuilder.append(line).append(NEW_LINE));

        replacements.forEach((placeholder, actual) -> {
            int index = stringBuilder.indexOf(placeholder);
            while (index != -1) {
                stringBuilder.replace(index, index + placeholder.length(), actual);
                index += actual.length();
                index = stringBuilder.indexOf(placeholder, index);
            }
        });

        return stringBuilder;
    }

    private static final String INDENT = "    ";
    private static final String NEW_LINE = System.lineSeparator();

    @VisibleForTesting
    static String getFilePathFromMavenName(String group, String artifactId) {
        return String.format(Locale.US, "%s/%s/", group.replaceAll("\\.", "/"), artifactId);
    }

    public static class HttpRepoRulesMacroWriter implements RuleWriter {

        private final File outputFile;
        private final String macroName;

        public HttpRepoRulesMacroWriter(final File outputFile, final String macroName) {
            this.outputFile = outputFile;
            this.macroName = macroName;
        }

        @Override
        public void write(Collection<Target> targets) throws IOException {
            targets = SortTargetsByName.sort(targets);

            StringBuilder httpRulesText = readTemplate(
                    "dependencies-http-repo-rules.bzl.template",
                    Collections.singletonMap("{{generate_workspace_rules}}", macroName));

            try (final OutputStreamWriter fileWriter =
                    new OutputStreamWriter(
                            new FileOutputStream(outputFile, true), Charsets.UTF_8)) {
                fileWriter.append(httpRulesText.toString()).append(NEW_LINE);

                if (targets.isEmpty()) {
                    fileWriter.append(INDENT).append("pass");
                } else {
                    for (Iterator<Target> iterator = targets.iterator(); iterator.hasNext(); ) {
                        Target target = iterator.next();
                        fileWriter
                                .append(INDENT)
                                .append("# from ")
                                .append(target.getMavenCoordinates())
                                .append(NEW_LINE);
                        fileWriter.append(target.outputString(INDENT)).append(NEW_LINE);
                        if (iterator.hasNext()) fileWriter.append(NEW_LINE);
                    }
                }
            }
        }
    }

    public static class TransitiveRulesMacroWriter implements RuleWriter {

        private static final String KOTLIN_LIB_MACRO_NAME = "kotlin_jar_support";
        private final File outputFile;
        private final String macroName;

        public TransitiveRulesMacroWriter(final File outputFile, final String macroName) {
            this.outputFile = outputFile;
            this.macroName = macroName;
        }

        @Override
        public void write(Collection<Target> targets) throws IOException {
            targets = SortTargetsByName.sort(targets);

            StringBuilder supportFunctions = readTemplate(
                    "dependencies-targets-support-functions.bzl.template",
                    Collections.singletonMap("{{kotlin_jar_support}}", KOTLIN_LIB_MACRO_NAME));
            StringBuilder targetsMacro = readTemplate(
                    "dependencies-targets-macro.bzl.template",
                    Collections.singletonMap("{{generate_transitive_dependency_targets}}", macroName));

            try (final OutputStreamWriter fileWriter =
                    new OutputStreamWriter(
                            new FileOutputStream(outputFile, true), Charsets.UTF_8)) {
                fileWriter.append(NEW_LINE);
                fileWriter.append(supportFunctions.toString()).append(NEW_LINE);
                fileWriter.append(targetsMacro.toString()).append(NEW_LINE);

                if (targets.isEmpty()) {
                    fileWriter.append(INDENT).append("pass");
                } else {
                    for (Iterator<Target> iterator = targets.iterator(); iterator.hasNext(); ) {
                        Target target = iterator.next();
                        fileWriter
                                .append(INDENT)
                                .append("# from ")
                                .append(target.getMavenCoordinates())
                                .append(NEW_LINE);

                        if (target.getRuleName().equals(KOTLIN_LIB_MACRO_NAME)) {
                            target.addVariable("kt_jvm_import", "kt_jvm_import");
                        }
                        fileWriter.append(target.outputString(INDENT)).append(NEW_LINE);
                        if (iterator.hasNext()) fileWriter.append(NEW_LINE);
                    }
                }
            }
        }
    }

    public static class TransitiveRulesAliasWriter implements RuleWriter {

        private final File baseFolder;
        private final String pathToTransitiveRulesPackage;

        public TransitiveRulesAliasWriter(
                final File baseFolder, final String pathToTransitiveRulesPackage) {
            this.baseFolder = baseFolder;
            this.pathToTransitiveRulesPackage = pathToTransitiveRulesPackage;
        }

        private static String versionlessMaven(Target target) {
            final String[] mavenCoordinates = target.getMavenCoordinates().split(":", -1);
            return String.format(Locale.US, "%s:%s", mavenCoordinates[0], mavenCoordinates[1]);
        }

        @Override
        public void write(final Collection<Target> targets) throws IOException {
            // Grouping by maven coordinates
            final Map<String, List<Target>> publicTargets =
                    targets.stream()
                            .filter(Target::isPublic)
                            .collect(
                                    Collectors.groupingBy(
                                            TransitiveRulesAliasWriter::versionlessMaven));

            for (Map.Entry<String, List<Target>> entry : publicTargets.entrySet()) {
                String key = entry.getKey();
                List<Target> packageTargets = entry.getValue();
                final String[] mavenCoordinates = key.split(":", -1);
                final File buildFileFolder =
                        new File(
                                baseFolder,
                                getFilePathFromMavenName(mavenCoordinates[0], mavenCoordinates[1]));
                if (!buildFileFolder.exists() && !buildFileFolder.mkdirs()) {
                    throw new IOException(
                            "Failed to create folder " + buildFileFolder.getAbsolutePath());
                }

                try (final OutputStreamWriter fileWriter =
                        new OutputStreamWriter(
                                new FileOutputStream(
                                        new File(buildFileFolder, "BUILD.bazel"), false),
                                Charsets.UTF_8)) {
                    fileWriter.append(readTemplate("dependencies-sub-folder-header.bzl.template",
                            Collections.singletonMap("{{MVN_COORDINATES}}", key))).append(NEW_LINE);

                    for (int targetIndex = 0, packageTargetsSize = packageTargets.size(); targetIndex < packageTargetsSize; targetIndex++) {
                        Target target = packageTargets.get(targetIndex);
                        fileWriter
                                .append(
                                        new Target(
                                                target.getMavenCoordinates(),
                                                "alias",
                                                target.getNameSpacedTargetName())
                                                .addString(
                                                        "actual",
                                                        String.format(
                                                                Locale.US,
                                                                "//%s:%s",
                                                                pathToTransitiveRulesPackage,
                                                                target.getTargetName()))
                                                .setPublicVisibility()
                                                .outputString(""))
                                .append(NEW_LINE);

                        if (targetIndex != packageTargetsSize - 1) {
                            fileWriter.append(NEW_LINE);
                        }
                    }
                }
            }
        }
    }
}
