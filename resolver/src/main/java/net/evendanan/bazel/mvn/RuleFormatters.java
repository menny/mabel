package net.evendanan.bazel.mvn;

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

public class RuleFormatters {

    public static final String RULE_INDENT = "    ";

    public static class CompositeFormatter implements RuleFormatter {

        private final Collection<RuleFormatter> ruleFormatters;

        CompositeFormatter(RuleFormatter... ruleFormatters) {
            this(Arrays.asList(ruleFormatters));
        }

        CompositeFormatter(Collection<RuleFormatter> ruleFormatters) {
            this.ruleFormatters = ImmutableList.copyOf(ruleFormatters);
        }

        @Override
        public String formatRule(final Rule rule) {
            StringBuilder builder = new StringBuilder();
            ruleFormatters.forEach(formatter -> builder.append(formatter.formatRule(rule)));

            return builder.toString();
        }
    }

    @VisibleForTesting
    static final RuleFormatter JAVA_IMPORT = rule -> {
        StringBuilder builder = new StringBuilder();
        addJavaImportRule(RULE_INDENT, rule, "", builder);
        builder.append('\n');
        addAlias(RULE_INDENT, builder, rule);

        return builder.toString();
    };

    private static void addJavaImportRule(String indent, Rule rule, String postFix, final StringBuilder builder) {
        new Target("native.java_import", rule.mavenGeneratedName() + postFix)
            .addList("jars", Collections.singletonList(String.format(Locale.US, "@%s//file", rule.mavenGeneratedName())))
            .addList("deps", convertRulesToStrings(rule.getDeps()))
            .addList("exports", convertRulesToStrings(rule.getExportDeps()))
            .addList("runtime_deps", convertRulesToStrings(rule.getRuntimeDeps()))
            .outputTarget(indent, builder);
    }

    @VisibleForTesting
    static final RuleFormatter KOTLIN_IMPORT = rule -> {
        StringBuilder builder = new StringBuilder();
        //In case the developer did not provide a kt_* impl, we'll try to use java_*, should work.
        builder.append(RULE_INDENT).append("if kt_jvm_import == None:\n");
        addJavaImportRule(RULE_INDENT + RULE_INDENT, rule, "", builder);

        builder.append(RULE_INDENT).append("else:\n");
        //In case the developer provide a kt_* impl we'll use them.
        new Target("kt_jvm_import", rule.mavenGeneratedName() + "_kotlin_jar")
            .addList("jars", Collections.singleton(String.format(Locale.US, "@%s//file", rule.mavenGeneratedName())))
            .outputTarget(RULE_INDENT + RULE_INDENT, builder);

        builder.append('\n');

        final Set<String> depsWithImportedJar = new HashSet<>(convertRulesToStrings(rule.getDeps()));
        depsWithImportedJar.add(":" + rule.mavenGeneratedName() + "_kotlin_jar");
        depsWithImportedJar.addAll(convertRulesToStrings(rule.getRuntimeDeps()));

        final Set<String> exportsWithImportedJar = new HashSet<>(convertRulesToStrings(rule.getExportDeps()));
        exportsWithImportedJar.add(":" + rule.mavenGeneratedName() + "_kotlin_jar");

        new Target("kt_jvm_library", rule.mavenGeneratedName())
            .addList("runtime_deps", depsWithImportedJar)
            .addList("exports", exportsWithImportedJar)
            .outputTarget(RULE_INDENT + RULE_INDENT, builder);

        builder.append('\n');
        addAlias(RULE_INDENT, builder, rule);

        return builder.toString();
    };

    static class JavaPluginFormatter implements RuleFormatter {

        private static final String API_POST_FIX = "_generate_api";
        private final List<String> processorClasses;

        JavaPluginFormatter(final Collection<String> processorClasses) {
            this.processorClasses = ImmutableList.copyOf(processorClasses);
        }

        @VisibleForTesting
        List<String> getProcessorClasses() {
            return processorClasses;
        }

        @Override
        public String formatRule(final Rule rule) {
            StringBuilder builder = new StringBuilder();

            Collection<String> deps = convertRulesToStrings(rule.getDeps());
            deps.add(":" + rule.mavenGeneratedName() + "_java_plugin_lib");
            addJavaImportRule(RULE_INDENT, rule, "_java_plugin_lib", builder);

            for (int processorClassIndex = 0; processorClassIndex < processorClasses.size(); processorClassIndex++) {
                final String processorClass = processorClasses.get(processorClassIndex);

                new Target("native.java_plugin", rule.mavenGeneratedName() + "_" + processorClassIndex)
                    .addString("processor_class", processorClass)
                    .addInt("generates_api", 0)
                    .addList("deps", deps)
                    .outputTarget(RULE_INDENT, builder);

                new Target("native.java_plugin", rule.mavenGeneratedName() + API_POST_FIX + "_" + processorClassIndex)
                    .addString("processor_class", processorClass)
                    .addInt("generates_api", 1)
                    .addList("deps", deps)
                    .outputTarget(RULE_INDENT, builder);
            }

            //composite libraries
            new Target("native.java_library", rule.mavenGeneratedName())
                //since there are no sources in this library, we put all deps as runtime_deps
                .addList("runtime_deps", deps)
                .addList("exported_plugins", IntStream.range(0, processorClasses.size())
                    .mapToObj(index -> ":" + rule.mavenGeneratedName() + "_" + index)
                    .collect(Collectors.toList()))
                .outputTarget(RULE_INDENT, builder);

            new Target("native.java_library", rule.mavenGeneratedName() + API_POST_FIX)
                //since there are no sources in this library, we put all deps as runtime_deps
                .addList("runtime_deps", deps)
                .addList("exported_plugins", IntStream.range(0, processorClasses.size())
                    .mapToObj(index -> ":" + rule.mavenGeneratedName() + API_POST_FIX + "_" + index)
                    .collect(Collectors.toList()))
                .outputTarget(RULE_INDENT, builder);

            builder.append('\n');
            addAlias(RULE_INDENT, builder, rule);
            addAlias(RULE_INDENT, builder, rule, API_POST_FIX);

            return builder.toString();
        }
    }

    @VisibleForTesting
    static final RuleFormatter AAR_IMPORT = rule -> {
        StringBuilder builder = new StringBuilder();

        final Set<Rule> deps = new HashSet<>(rule.getDeps());
        deps.addAll(rule.getRuntimeDeps());

        new Target("native.aar_import", rule.mavenGeneratedName())
            .addString("aar", String.format(Locale.US, "@%s//file", rule.mavenGeneratedName()))
            .addList("deps", convertRulesToStrings(deps))
            .addList("exports", convertRulesToStrings(rule.getExportDeps()))
            .outputTarget(RULE_INDENT, builder);

        builder.append('\n');
        addAlias(RULE_INDENT, builder, rule);

        return builder.toString();
    };

    public static final RuleFormatter HTTP_FILE = rule -> {
        StringBuilder builder = new StringBuilder(0);
        for (String parent : rule.getParents()) {
            builder.append(RULE_INDENT).append("# ").append(parent).append('\n');
        }
        new Target("http_file", rule.mavenGeneratedName())
            .addList("urls", Collections.singleton(rule.getUrl()))
            .addString("downloaded_file_path", getFilenameFromUrl(rule.getUrl()))
            .outputTarget(RULE_INDENT, builder);

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

    private static void addAlias(String indent, StringBuilder builder, Rule rule) {
        addAlias(indent, builder, rule, "");
    }

    private static void addAlias(String indent, StringBuilder builder, Rule rule, String postFix) {
        new Target("native.alias", rule.safeRuleFriendlyName() + postFix)
            .addString("actual", rule.mavenGeneratedName() + postFix)
            .setPublicVisibility()
            .outputTarget(indent, builder);
    }
}
