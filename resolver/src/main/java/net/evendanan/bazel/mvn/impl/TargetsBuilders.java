package net.evendanan.bazel.mvn.impl;

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
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.TargetsBuilder;

public class TargetsBuilders {

    public static final TargetsBuilder HTTP_FILE = rule -> Collections.singletonList(new Target(rule.mavenCoordinates(), "http_file", rule.mavenGeneratedName())
            .addList("urls", Collections.singleton(rule.getUrl()))
            .addString("downloaded_file_path", getFilenameFromUrl(rule.getUrl())));
    static final TargetsBuilder JAVA_IMPORT = rule -> {
        List<Target> targets = new ArrayList<>();
        targets.add(addJavaImportRule(false, rule, ""));
        targets.add(addAlias(false, rule));

        return targets;
    };
    static final TargetsBuilder NATIVE_JAVA_IMPORT = rule -> {
        List<Target> targets = new ArrayList<>();
        targets.add(addJavaImportRule(true, rule, ""));
        targets.add(addAlias(true, rule));

        return targets;
    };
    static final TargetsBuilder KOTLIN_IMPORT = new KotlinImport(false);
    static final TargetsBuilder NATIVE_KOTLIN_IMPORT = new KotlinImport(true);
    @VisibleForTesting
    static final TargetsBuilder AAR_IMPORT = new AarImport(false);
    @VisibleForTesting
    static final TargetsBuilder NATIVE_AAR_IMPORT = new AarImport(true);

    private static Target addJavaImportRule(boolean asNative, Rule rule, String postFix) {
        return new Target(rule.mavenCoordinates(), asNative ? "native.java_import":"java_import", rule.mavenGeneratedName() + postFix)
                .addList("jars", Collections.singletonList(String.format(Locale.US, "@%s//file", rule.mavenGeneratedName())))
                .addList("deps", convertRulesToStrings(rule.getDeps()))
                .addList("exports", convertRulesToStrings(rule.getExportDeps()))
                .addList("runtime_deps", convertRulesToStrings(rule.getRuntimeDeps()));
    }

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

    private static Target addAlias(boolean asNative, Rule rule) {
        return addAlias(asNative, rule, "");
    }

    private static Target addAlias(boolean asNative, Rule rule, String postFix) {
        return new Target(rule.mavenCoordinates(), asNative ? "native.alias":"alias", rule.safeRuleFriendlyName() + postFix)
                .addString("actual", String.format(Locale.US, ":%s%s", rule.mavenGeneratedName(), postFix))
                .setPublicVisibility();
    }

    public static class CompositeBuilder implements TargetsBuilder {

        private final Collection<TargetsBuilder> targetsBuilders;

        CompositeBuilder(TargetsBuilder... targetsBuilders) {
            this(Arrays.asList(targetsBuilders));
        }

        CompositeBuilder(Collection<TargetsBuilder> targetsBuilders) {
            this.targetsBuilders = ImmutableList.copyOf(targetsBuilders);
        }

        @Override
        public List<Target> buildTargets(Rule rule) {
            return targetsBuilders.stream().flatMap(builder -> builder.buildTargets(rule).stream()).collect(Collectors.toList());
        }
    }

    public static class KotlinImport implements TargetsBuilder {

        private final boolean asNative;

        KotlinImport(final boolean asNative) {
            this.asNative = asNative;
        }

        @Override
        public List<Target> buildTargets(final Rule rule) {
            List<Target> targets = new ArrayList<>();

            targets.add(new Target(rule.mavenCoordinates(), "kotlin_jar_support", rule.mavenGeneratedName())
                    .addList("deps", convertRulesToStrings(rule.getDeps()))
                    .addList("exports", convertRulesToStrings(rule.getExportDeps()))
                    .addList("runtime_deps", convertRulesToStrings(rule.getRuntimeDeps()))
                    .addString("jar", String.format(Locale.US, "@%s//file", rule.mavenGeneratedName())));

            targets.add(addAlias(asNative, rule));

            return targets;
        }
    }

    public static class JavaPluginFormatter implements TargetsBuilder {

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
        public List<Target> buildTargets(Rule rule) {
            List<Target> targets = new ArrayList<>();

            Collection<String> deps = convertRulesToStrings(rule.getDeps());
            deps.add(":" + rule.mavenGeneratedName() + "_java_plugin_lib");
            targets.add(addJavaImportRule(asNative, rule, "_java_plugin_lib"));

            for (int processorClassIndex = 0; processorClassIndex < processorClasses.size(); processorClassIndex++) {
                final String processorClass = processorClasses.get(processorClassIndex);

                targets.add(new Target(rule.mavenCoordinates(), asNative ? "native.java_plugin":"java_plugin", rule.mavenGeneratedName() + "_" + processorClassIndex)
                        .addString("processor_class", processorClass)
                        .addInt("generates_api", 0)
                        .addList("deps", deps));

                targets.add(new Target(rule.mavenCoordinates(), asNative ? "native.java_plugin":"java_plugin", rule.mavenGeneratedName() + API_POST_FIX + "_" + processorClassIndex)
                        .addString("processor_class", processorClass)
                        .addInt("generates_api", 1)
                        .addList("deps", deps));
            }

            //composite libraries
            targets.add(new Target(rule.mavenCoordinates(), asNative ? "native.java_library":"java_library", rule.mavenGeneratedName())
                    //since there are no sources in this library, we put all deps as runtime_deps
                    .addList("runtime_deps", deps)
                    .addList("exported_plugins", IntStream.range(0, processorClasses.size())
                            .mapToObj(index -> ":" + rule.mavenGeneratedName() + "_" + index)
                            .collect(Collectors.toList())));

            targets.add(new Target(rule.mavenCoordinates(), asNative ? "native.java_library":"java_library", rule.mavenGeneratedName() + API_POST_FIX)
                    //since there are no sources in this library, we put all deps as runtime_deps
                    .addList("runtime_deps", deps)
                    .addList("exported_plugins", IntStream.range(0, processorClasses.size())
                            .mapToObj(index -> ":" + rule.mavenGeneratedName() + API_POST_FIX + "_" + index)
                            .collect(Collectors.toList())));

            targets.add(addAlias(asNative, rule));
            targets.add(addAlias(asNative, rule, API_POST_FIX));

            return targets;
        }
    }

    static class AarImport implements TargetsBuilder {

        private final boolean asNative;

        AarImport(final boolean asNative) {
            this.asNative = asNative;
        }

        @Override
        public List<Target> buildTargets(final Rule rule) {
            List<Target> targets = new ArrayList<>();
            final Set<Rule> deps = new HashSet<>(rule.getDeps());
            deps.addAll(rule.getRuntimeDeps());

            targets.add(new Target(rule.mavenCoordinates(), asNative ? "native.aar_import":"aar_import", rule.mavenGeneratedName())
                    .addString("aar", String.format(Locale.US, "@%s//file", rule.mavenGeneratedName()))
                    .addList("deps", convertRulesToStrings(deps))
                    .addList("exports", convertRulesToStrings(rule.getExportDeps())));

            targets.add(addAlias(asNative, rule));

            return targets;
        }
    }
}
