package net.evendanan.bazel.mvn.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.devtools.bazel.workspace.maven.Rule;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.evendanan.bazel.mvn.api.RuleFormatter;
import net.evendanan.bazel.mvn.api.Target;

public class RuleFormatters {

    private static final String RULE_INDENT_BASE = "    ";

    static StringBuilder addIndent(int times, StringBuilder builder) {
        while (times-- > 0) {
            builder.append(RULE_INDENT_BASE);
        }
        return builder;
    }

    public static class CompositeFormatter implements RuleFormatter {

        private final Collection<RuleFormatter> ruleFormatters;

        CompositeFormatter(RuleFormatter... ruleFormatters) {
            this(Arrays.asList(ruleFormatters));
        }

        CompositeFormatter(Collection<RuleFormatter> ruleFormatters) {
            this.ruleFormatters = ImmutableList.copyOf(ruleFormatters);
        }

        @Override
        public String formatRule(String baseIndent, Rule rule) {
            StringBuilder builder = new StringBuilder();
            ruleFormatters.forEach(formatter -> builder.append(formatter.formatRule(baseIndent, rule)));

            return builder.toString();
        }
    }

    static final RuleFormatter JAVA_IMPORT = (baseIndent, rule) -> {
        StringBuilder builder = new StringBuilder();
        addJavaImportRule(false, baseIndent, rule, "", builder);
        builder.append('\n');
        addAlias(false, baseIndent, builder, rule);

        return builder.toString();
    };

    static final RuleFormatter NATIVE_JAVA_IMPORT = (baseIndent, rule) -> {
        StringBuilder builder = new StringBuilder();
        addJavaImportRule(true, baseIndent, rule, "", builder);
        builder.append('\n');
        addAlias(true, baseIndent, builder, rule);

        return builder.toString();
    };

    private static void addJavaImportRule(boolean asNative, String indent, Rule rule, String postFix, final StringBuilder builder) {
        new Target(asNative ? "native.java_import" : "java_import", rule.mavenGeneratedName() + postFix)
            .addList("jars", Collections.singletonList(String.format(Locale.US, "@%s//file", rule.mavenGeneratedName())))
            .addList("deps", convertRulesToStrings(rule.getDeps()))
            .addList("exports", convertRulesToStrings(rule.getExportDeps()))
            .addList("runtime_deps", convertRulesToStrings(rule.getRuntimeDeps()))
            .outputTarget(indent, builder);
    }

    public static class KotlinImport implements RuleFormatter {

        private final boolean asNative;

        KotlinImport(final boolean asNative) {this.asNative = asNative;}

        @Override
        public String formatRule(final String baseIndent, final Rule rule) {
            StringBuilder builder = new StringBuilder();
            //In case the developer did not provide a kt_* impl, we'll try to use java_*, should work.
            builder.append(baseIndent).append("if kt_jvm_import == None:\n");
            addJavaImportRule(asNative, baseIndent + RULE_INDENT_BASE, rule, "", builder);

            builder.append(baseIndent).append("else:\n");
            //In case the developer provide a kt_* impl we'll use them.
            new Target("kt_jvm_import", rule.mavenGeneratedName() + "_kotlin_jar")
                .addList("jars", Collections.singleton(String.format(Locale.US, "@%s//file", rule.mavenGeneratedName())))
                .outputTarget(baseIndent + RULE_INDENT_BASE, builder);

            builder.append('\n');

            final Set<String> depsWithImportedJar = new HashSet<>(convertRulesToStrings(rule.getDeps()));
            depsWithImportedJar.add(":" + rule.mavenGeneratedName() + "_kotlin_jar");
            depsWithImportedJar.addAll(convertRulesToStrings(rule.getRuntimeDeps()));

            final Set<String> exportsWithImportedJar = new HashSet<>(convertRulesToStrings(rule.getExportDeps()));
            exportsWithImportedJar.add(":" + rule.mavenGeneratedName() + "_kotlin_jar");

            new Target("kt_jvm_library", rule.mavenGeneratedName())
                .addList("runtime_deps", depsWithImportedJar)
                .addList("exports", exportsWithImportedJar)
                .outputTarget(baseIndent + RULE_INDENT_BASE, builder);

            builder.append('\n');
            addAlias(asNative, baseIndent, builder, rule);

            return builder.toString();
        }
    }

    static final RuleFormatter KOTLIN_IMPORT = new KotlinImport(false);
    static final RuleFormatter NATIVE_KOTLIN_IMPORT = new KotlinImport(true);

    public static class JavaPluginFormatter implements RuleFormatter {

        private static final String API_POST_FIX = "_generate_api";
        private final List<String> processorClasses;
        private final boolean asNative;

        JavaPluginFormatter(final boolean asNative, final Collection<String> processorClasses) {
            this.asNative = asNative;
            this.processorClasses = ImmutableList.copyOf(processorClasses);
        }

        @VisibleForTesting
        List<String> getProcessorClasses() {
            return processorClasses;
        }

        @VisibleForTesting
        boolean getIsNative() {
            return asNative;
        }

        @Override
        public String formatRule(String indent, Rule rule) {
            StringBuilder builder = new StringBuilder();

            Collection<String> deps = convertRulesToStrings(rule.getDeps());
            deps.add(":" + rule.mavenGeneratedName() + "_java_plugin_lib");
            addJavaImportRule(asNative, indent, rule, "_java_plugin_lib", builder);

            for (int processorClassIndex = 0; processorClassIndex < processorClasses.size(); processorClassIndex++) {
                final String processorClass = processorClasses.get(processorClassIndex);

                new Target(asNative ? "native.java_plugin" : "java_plugin", rule.mavenGeneratedName() + "_" + processorClassIndex)
                    .addString("processor_class", processorClass)
                    .addInt("generates_api", 0)
                    .addList("deps", deps)
                    .outputTarget(indent, builder);

                new Target(asNative ? "native.java_plugin" : "java_plugin", rule.mavenGeneratedName() + API_POST_FIX + "_" + processorClassIndex)
                    .addString("processor_class", processorClass)
                    .addInt("generates_api", 1)
                    .addList("deps", deps)
                    .outputTarget(indent, builder);
            }

            //composite libraries
            new Target(asNative ? "native.java_library" : "java_library", rule.mavenGeneratedName())
                //since there are no sources in this library, we put all deps as runtime_deps
                .addList("runtime_deps", deps)
                .addList("exported_plugins", IntStream.range(0, processorClasses.size())
                    .mapToObj(index -> ":" + rule.mavenGeneratedName() + "_" + index)
                    .collect(Collectors.toList()))
                .outputTarget(indent, builder);

            new Target(asNative ? "native.java_library" : "java_library", rule.mavenGeneratedName() + API_POST_FIX)
                //since there are no sources in this library, we put all deps as runtime_deps
                .addList("runtime_deps", deps)
                .addList("exported_plugins", IntStream.range(0, processorClasses.size())
                    .mapToObj(index -> ":" + rule.mavenGeneratedName() + API_POST_FIX + "_" + index)
                    .collect(Collectors.toList()))
                .outputTarget(indent, builder);

            builder.append('\n');
            addAlias(asNative, indent, builder, rule);
            addAlias(asNative, indent, builder, rule, API_POST_FIX);

            return builder.toString();
        }
    }

    static class AarImport implements RuleFormatter {

        private final boolean asNative;

        AarImport(final boolean asNative) {this.asNative = asNative;}

        @Override
        public String formatRule(final String baseIndent, final Rule rule) {
            StringBuilder builder = new StringBuilder();

            final Set<Rule> deps = new HashSet<>(rule.getDeps());
            deps.addAll(rule.getRuntimeDeps());

            new Target(asNative ? "native.aar_import" : "aar_import", rule.mavenGeneratedName())
                .addString("aar", String.format(Locale.US, "@%s//file", rule.mavenGeneratedName()))
                .addList("deps", convertRulesToStrings(deps))
                .addList("exports", convertRulesToStrings(rule.getExportDeps()))
                .outputTarget(baseIndent, builder);

            builder.append('\n');
            addAlias(asNative, baseIndent, builder, rule);

            return builder.toString();
        }
    }

    @VisibleForTesting
    static final RuleFormatter AAR_IMPORT = new AarImport(false);
    @VisibleForTesting
    static final RuleFormatter NATIVE_AAR_IMPORT = new AarImport(true);

    static final RuleFormatter HTTP_FILE = (baseIndent, rule) -> {
        StringBuilder builder = new StringBuilder(0);
        for (String parent : rule.getParents()) {
            builder.append(baseIndent).append("# ").append(parent).append('\n');
        }
        new Target("http_file", rule.mavenGeneratedName())
            .addList("urls", Collections.singleton(rule.getUrl()))
            .addString("downloaded_file_path", getFilenameFromUrl(rule.getUrl()))
            .outputTarget(baseIndent, builder);

        return builder.toString();
    };

    private static String getFilenameFromUrl(String url) {
        int lastPathSeparator = url.lastIndexOf("/");
        if (lastPathSeparator < 0) {
            throw new IllegalArgumentException("Could not parse filename out of URL '" + url + "'");
        }

        return url.substring(lastPathSeparator + 1);
    }

    private static Collection<String> convertRulesToStrings(final Collection<Rule> labels) {
        return labels.stream()
            //using the friendly name (without version), so the mapping will be done via the alias mechanism.
            .map(Rule::safeRuleFriendlyName)
            .map(ruleName -> String.format(Locale.US, ":%s", ruleName))
            .collect(Collectors.toList());
    }

    private static void addAlias(boolean asNative, String indent, StringBuilder builder, Rule rule) {
        addAlias(asNative, indent, builder, rule, "");
    }

    private static void addAlias(boolean asNative, String indent, StringBuilder builder, Rule rule, String postFix) {
        new Target(asNative ? "native.alias" : "alias", rule.safeRuleFriendlyName() + postFix)
            .addString("actual", rule.mavenGeneratedName() + postFix)
            .setPublicVisibility()
            .outputTarget(indent, builder);
    }
}
