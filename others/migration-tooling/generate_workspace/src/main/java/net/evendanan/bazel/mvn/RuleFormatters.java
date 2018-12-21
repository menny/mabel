package net.evendanan.bazel.mvn;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.devtools.bazel.workspace.maven.Rule;
import java.util.ArrayList;
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
    private static final String RULE_ARGUMENTS_INDENT = RULE_INDENT + RULE_INDENT;

    public static class CompositeFormatter implements RuleFormatter {

        private final Collection<RuleFormatter> ruleFormatters;

        public CompositeFormatter(RuleFormatter... ruleFormatters) {
            this(Arrays.asList(ruleFormatters));
        }

        public CompositeFormatter(Collection<RuleFormatter> ruleFormatters) {
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
        builder.append(RULE_INDENT).append("native.java_import").append("(\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.mavenGeneratedName()).append("\",\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("jars = [\"@").append(rule.safeRuleFriendlyName()).append("//file\"],\n");
        addListArgument(builder, "deps", rule.getDeps());
        addListArgument(builder, "exports", rule.getExportDeps());
        addListArgument(builder, "runtime_deps", rule.getRuntimeDeps());

        builder.append(RULE_INDENT).append(")\n");

        addAlias(builder, rule);

        return builder.toString();
    };

    @VisibleForTesting
    static final RuleFormatter KOTLIN_IMPORT = rule -> {
        StringBuilder builder = new StringBuilder();
        builder.append(RULE_INDENT).append("kt_jvm_import").append("(\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.mavenGeneratedName()).append("\",\n");

        final Set<String> deps = new HashSet<>(convertRulesToStrings(rule.getDeps()));
        deps.add(String.format(Locale.US, "@%s//file", rule.safeRuleFriendlyName()));

        addStringListArgument(builder, "jars", deps);
        addListArgument(builder, "runtime_deps", rule.getRuntimeDeps());

        builder.append(RULE_INDENT).append(")\n");

        addAlias(builder, rule);

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
            final Set<Rule> deps = new HashSet<>();
            deps.addAll(rule.getDeps());
            deps.addAll(rule.getRuntimeDeps());
            deps.addAll(rule.getExportDeps());

            StringBuilder builder = new StringBuilder();
            for (int processorClassIndex = 0; processorClassIndex < processorClasses.size(); processorClassIndex++) {
                final String processorClass = processorClasses.get(processorClassIndex);

                builder.append(RULE_INDENT).append("native.java_plugin").append("(\n");
                builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.mavenGeneratedName()).append("_").append(processorClassIndex).append("\",\n");
                builder.append(RULE_ARGUMENTS_INDENT).append("processor_class = \"").append(processorClass).append("\",\n");
                builder.append(RULE_ARGUMENTS_INDENT).append("generates_api = 0").append(",\n");
                addListArgument(builder, "deps", deps);
                builder.append(RULE_ARGUMENTS_INDENT).append(")\n");

                builder.append(RULE_INDENT).append("native.java_plugin").append("(\n");
                builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.mavenGeneratedName()).append(API_POST_FIX).append("_").append(processorClassIndex)
                    .append("\",\n");
                builder.append(RULE_ARGUMENTS_INDENT).append("processor_class = \"").append(processorClass).append("\",\n");
                builder.append(RULE_ARGUMENTS_INDENT).append("generates_api = 1").append(",\n");
                addListArgument(builder, "deps", deps);
                builder.append(RULE_ARGUMENTS_INDENT).append(")\n");
            }

            //composite plugins
            builder.append(RULE_INDENT).append("native.java_plugin").append("(\n");
            builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.mavenGeneratedName()).append("\",\n");
            builder.append(RULE_ARGUMENTS_INDENT).append("generates_api = 0").append(",\n");
            addListArgument(builder, "deps", deps);
            addStringListArgument(builder, "plugins", IntStream.range(0, processorClasses.size())
                .mapToObj(index -> rule.mavenGeneratedName() + "_" + index)
                .collect(Collectors.toList()));
            builder.append(RULE_ARGUMENTS_INDENT).append(")\n");

            builder.append(RULE_INDENT).append("native.java_plugin").append("(\n");
            builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.mavenGeneratedName()).append(API_POST_FIX).append("\",\n");
            builder.append(RULE_ARGUMENTS_INDENT).append("generates_api = 1").append(",\n");
            addListArgument(builder, "deps", deps);
            addStringListArgument(builder, "plugins", IntStream.range(0, processorClasses.size())
                .mapToObj(index -> rule.mavenGeneratedName() + API_POST_FIX + "_" + index)
                .collect(Collectors.toList()));
            builder.append(RULE_ARGUMENTS_INDENT).append(")\n");

            addAlias(builder, rule);
            addAlias(builder, rule, API_POST_FIX);

            return builder.toString();
        }
    }

    @VisibleForTesting
    static final RuleFormatter AAR_IMPORT = rule -> {
        StringBuilder builder = new StringBuilder();
        builder.append(RULE_INDENT).append("native.aar_import").append("(\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.mavenGeneratedName()).append("\",\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("aar = \"@").append(rule.safeRuleFriendlyName()).append("//file\",\n");

        final Set<Rule> deps = new HashSet<>();
        deps.addAll(rule.getDeps());
        deps.addAll(rule.getRuntimeDeps());
        addListArgument(builder, "deps", deps);
        addListArgument(builder, "exports", rule.getExportDeps());

        builder.append(RULE_INDENT).append(")\n");

        addAlias(builder, rule);

        return builder.toString();
    };

    public static final RuleFormatter HTTP_FILE = rule -> {
        StringBuilder builder = new StringBuilder(0);
        for (String parent : rule.getParents()) {
            builder.append(RULE_INDENT).append("# ").append(parent).append('\n');
        }
        builder.append(RULE_INDENT).append("http_file").append("(\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.safeRuleFriendlyName()).append("\",\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("urls = [\"").append(rule.getUrl()).append("\"],\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("downloaded_file_path = \"").append(getFilenameFromUrl(rule.getUrl())).append("\",\n");
        builder.append(RULE_ARGUMENTS_INDENT).append(")");
        return builder.toString();
    };

    private static String getFilenameFromUrl(String url) {
        int lastPathSeparator = url.lastIndexOf("/");
        if (lastPathSeparator < 0) {
            throw new IllegalArgumentException("Could not parse filename out of URL '" + url + "'");
        }

        return url.substring(lastPathSeparator + 1);
    }

    private static void addListArgument(StringBuilder builder, String name, Collection<Rule> labels) {
        addStringListArgument(builder, name, convertRulesToStrings(labels));
    }

    private static Collection<String> convertRulesToStrings(final Collection<Rule> labels) {
        return labels.stream()
            .map(Rule::safeRuleFriendlyName)
            .map(ruleName -> String.format(Locale.US, ":%s", ruleName))
            .collect(Collectors.toList());
    }

    private static void addStringListArgument(StringBuilder builder, String name, Collection<String> labels) {
        if (!labels.isEmpty()) {
            ArrayList<String> sortedLabels = new ArrayList<>(labels);
            Collections.sort(sortedLabels);
            builder.append(RULE_ARGUMENTS_INDENT).append(name).append(" = [\n");
            for (String r : sortedLabels) {
                builder.append(RULE_ARGUMENTS_INDENT).append(RULE_INDENT).append("\"").append(r).append("\",\n");
            }
            builder.append(RULE_ARGUMENTS_INDENT).append("],\n");
        }
    }

    private static void addAlias(StringBuilder builder, Rule rule) {
        addAlias(builder, rule, "");
    }

    private static void addAlias(StringBuilder builder, Rule rule, String postFix) {
        builder.append(RULE_INDENT).append("native.alias(\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.safeRuleFriendlyName()).append(postFix).append("\",\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("actual = \"").append(rule.mavenGeneratedName()).append("\",\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("visibility = [\"//visibility:public\"],\n");
        builder.append(RULE_INDENT).append(")\n\n");
    }
}
