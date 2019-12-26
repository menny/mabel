package net.evendanan.bazel.mvn.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.RuleWriter;
import net.evendanan.bazel.mvn.api.Target;

public class RuleWriters {

    private static final String INDENT = "    ";
    private static final String NEW_LINE = System.lineSeparator();

    @VisibleForTesting
    static String getFilePathFromMavenName(String group, String artifactId) {
        return String.format(Locale.US, "%s/%s/",
                group.replaceAll("\\.", "/"), artifactId);
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

            try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(outputFile, true), Charsets.UTF_8)) {
                fileWriter.append("# Loading a drop-in replacement for native.http_file").append(NEW_LINE);
                fileWriter.append("load('@bazel_tools//tools/build_defs/repo:http.bzl', 'http_file')").append(NEW_LINE);
                fileWriter.append(NEW_LINE);

                fileWriter.append("# Repository rules macro to be run in the WORKSPACE file.").append(NEW_LINE);
                fileWriter.append("def ").append(macroName).append("():").append(NEW_LINE);
                if (targets.isEmpty()) {
                    fileWriter.append(INDENT).append("pass");
                } else {
                    for (Target target : targets) {
                        fileWriter.append(INDENT).append("# from ").append(target.getMavenCoordinates()).append(NEW_LINE);
                        fileWriter.append(target.outputString(INDENT)).append(NEW_LINE);
                    }
                }
                fileWriter.append(NEW_LINE);
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

            try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(outputFile, true), Charsets.UTF_8)) {
                fileWriter.append("# Transitive rules macro to be run in the BUILD.bazel file.").append(NEW_LINE);
                fileWriter.append("# If you use kt_* rules, you MUST provide the correct rule implementation when call this macro, if you decide").append(NEW_LINE);
                fileWriter.append("# not to provide those implementations we'll try to use java_* rules.").append(NEW_LINE);
                fileWriter.append(NEW_LINE);

                fileWriter.append("# This is a help macro to handle Kotlin rules.").append(NEW_LINE);
                fileWriter.append("def ").append(KOTLIN_LIB_MACRO_NAME).append("(name, deps, exports, runtime_deps, jar, java_import_impl, kt_jvm_import=None, kt_jvm_library=None):").append(NEW_LINE);
                fileWriter.append(INDENT).append("#In case the developer did not provide a kt_* impl, we'll try to use java_*, should work").append(NEW_LINE);
                fileWriter.append(INDENT).append("if kt_jvm_import == None:").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append("java_import_impl(name = name,").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append(INDENT).append("jars = [jar],").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append(INDENT).append("deps = deps,").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append(INDENT).append("exports = exports,").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append(INDENT).append("runtime_deps = runtime_deps,").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append(")").append(NEW_LINE);
                fileWriter.append(INDENT).append("else:").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append("kt_jvm_import(name = '{}_kotlin_jar'.format(name),").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append(INDENT).append("jars = [jar],").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append(")").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append("kt_jvm_library(name = name,").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append(INDENT).append("deps = deps + [':{}_kotlin_jar'.format(name)],").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append(INDENT).append("exports = exports + [':{}_kotlin_jar'.format(name)],").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append(INDENT).append("runtime_deps = runtime_deps,").append(NEW_LINE);
                fileWriter.append(INDENT).append(INDENT).append(")").append(NEW_LINE);
                fileWriter.append(NEW_LINE);

                fileWriter.append("# Macro to set up the transitive rules.").append(NEW_LINE);
                fileWriter.append("# You can provide your own implementation of java_import and aar_import. This can be used").append(NEW_LINE);
                fileWriter.append("# in cases where you need to shade (or jar_jar or jetify) your jars.").append(NEW_LINE);
                fileWriter.append("def ").append(macroName).append("(java_import_impl=native.java_import, aar_import_impl=native.aar_import, kt_jvm_import=None, kt_jvm_library=None):").append(NEW_LINE);
                if (targets.isEmpty()) {
                    fileWriter.append(INDENT).append("pass");
                } else {
                    for (Target target : targets) {
                        fileWriter.append(INDENT).append("# from ").append(target.getMavenCoordinates()).append(NEW_LINE);

                        if (target.getRuleName().equals(KOTLIN_LIB_MACRO_NAME)) {
                            target.addVariable("kt_jvm_import", "kt_jvm_import")
                                    .addVariable("kt_jvm_library", "kt_jvm_library");
                        }
                        fileWriter.append(target.outputString(INDENT)).append(NEW_LINE);
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

        private static String versionlessMaven(Target target) {
            final String[] mavenCoordinates = target.getMavenCoordinates().split(":", -1);
            return String.format(Locale.US, "%s:%s", mavenCoordinates[0], mavenCoordinates[1]);
        }

        @Override
        public void write(final Collection<Target> targets) throws IOException {
            //Grouping by maven coordinates
            final Map<String, List<Target>> publicTargets = targets.stream()
                    .filter(Target::isPublic)
                    .collect(Collectors.groupingBy(TransitiveRulesAliasWriter::versionlessMaven));

            for (Map.Entry<String, List<Target>> entry : publicTargets.entrySet()) {
                String key = entry.getKey();
                List<Target> packageTargets = entry.getValue();
                final Target defaultTarget = packageTargets.get(0);
                final String[] mavenCoordinates = key.split(":", -1);
                final File buildFileFolder = new File(baseFolder, getFilePathFromMavenName(mavenCoordinates[0], mavenCoordinates[1]));
                if (!buildFileFolder.exists() && !buildFileFolder.mkdirs()) {
                    throw new IOException("Failed to create folder " + buildFileFolder.getAbsolutePath());
                }

                try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(new File(buildFileFolder, "BUILD.bazel"), false), Charsets.UTF_8)) {
                    fileWriter.append("#Auto-generated by https://github.com/menny/mabel").append(NEW_LINE);
                    fileWriter.append("# for artifact ").append(key).append(NEW_LINE).append(NEW_LINE);

                    for (final Target target : packageTargets) {
                        fileWriter.append(new Target(target.getMavenCoordinates(), "alias", defaultTarget==target ? buildFileFolder.getName():target.getTargetName())
                                .addString("actual", String.format(Locale.US, "//%s:%s", pathToTransitiveRulesPackage, target.getTargetName()))
                                .setPublicVisibility()
                                .outputString(""))
                                .append(NEW_LINE);
                    }
                }
            }
        }
    }
}
