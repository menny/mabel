package net.evendanan.bazel.mvn.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.License;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.TargetsBuilder;

public class TargetsBuilders {

    public static final TargetsBuilder JAVA_IMPORT = dependency -> {
        List<Target> targets = new ArrayList<>();
        targets.add(addJavaImportRule(dependency));
        targets.add(addAlias(dependency));

        return targets;
    };
    static final TargetsBuilder KOTLIN_IMPORT = new KotlinImport();
    @VisibleForTesting
    static final TargetsBuilder AAR_IMPORT = new AarImport();

    private static Target addJavaImportRule(Dependency dependency) {
        final Target target = new Target(dependency.mavenCoordinates(), "java_import_impl", dependency.repositoryRuleName())
                .addList("jars", "pom".equalsIgnoreCase(dependency.packaging()) ?
                        Collections.emptyList()
                        :Collections.singletonList(String.format(Locale.ROOT, "@%s//file", dependency.repositoryRuleName())))
                .addList("tags", Collections.singletonList(String.format(Locale.ROOT, "maven_coordinates=%s", dependency.mavenCoordinates())))
                .addList("licenses", dependency.licenses().stream().map(License::toString).collect(Collectors.toList()))
                .addList("deps", convertRulesToStrings(dependency.dependencies()))
                .addList("exports", convertRulesToStrings(dependency.exports()))
                .addList("runtime_deps", convertRulesToStrings(dependency.runtimeDependencies()));
        if (!dependency.sourcesUrl().toASCIIString().isEmpty()) {
            target.addString("srcjar", String.format(Locale.US, "@%s__sources//file", dependency.repositoryRuleName()));
        }
        return target;
    }

    private static String getFilenameFromUrl(String url) {
        int lastPathSeparator = url.lastIndexOf("/");
        if (lastPathSeparator < 0) {
            throw new IllegalArgumentException("Could not parse filename out of URL '" + url + "'");
        }

        return url.substring(lastPathSeparator + 1);
    }

    private static Collection<String> convertRulesToStrings(final Collection<Dependency> dependencies) {
        return dependencies.stream()
                //using the friendly name (without version), so the mapping will be done via the alias mechanism.
                .map(Dependency::targetName)
                .map(name -> String.format(Locale.US, ":%s", name))
                .collect(Collectors.toList());
    }

    private static Target addAlias(Dependency dependency) {
        return addAlias(dependency, "");
    }

    private static Target addAlias(Dependency dependency, String postFix) {
        return new Target(dependency.mavenCoordinates(), "native.alias", dependency.targetName() + postFix)
                .addString("actual", String.format(Locale.US, ":%s%s", dependency.repositoryRuleName(), postFix))
                .setPublicVisibility();
    }

    public static class HttpTargetsBuilder implements TargetsBuilder {
        private final static char[] hexArray = "0123456789abcdef".toCharArray();
        private final boolean calculateSha;
        private final byte[] readBuffer;
        private final Function<Dependency, URI> downloader;

        public HttpTargetsBuilder(boolean calculateSha, Function<Dependency, URI> downloader) {
            this.calculateSha = calculateSha;
            this.readBuffer = calculateSha ? new byte[4096]:new byte[0];
            this.downloader = downloader;
        }

        @Override
        public List<Target> buildTargets(final Dependency dependency) {
            if ("pom".equalsIgnoreCase(dependency.packaging())) return Collections.emptyList();

            final Target jarTarget = new Target(dependency.mavenCoordinates(), "http_file", dependency.repositoryRuleName())
                    .addList("urls", Collections.singleton(dependency.url().toASCIIString()))
                    .addString("downloaded_file_path", getFilenameFromUrl(dependency.url().getPath()));

            if (calculateSha && !dependency.version().contains("SNAPSHOT")) {
                try (InputStream inputStream = downloader.apply(dependency).toURL().openStream()) {
                    final MessageDigest digest = MessageDigest.getInstance("SHA-256");

                    int bytesCount;
                    while ((bytesCount = inputStream.read(readBuffer))!=-1) {
                        digest.update(readBuffer, 0, bytesCount);
                    }

                    byte[] digestBytes = digest.digest();
                    char[] hexChars = new char[digestBytes.length * 2];
                    for (int digestByteIndex = 0; digestByteIndex < digestBytes.length; digestByteIndex++) {
                        int v = digestBytes[digestByteIndex] & 0xFF;
                        hexChars[digestByteIndex * 2] = hexArray[v >>> 4];
                        hexChars[digestByteIndex * 2 + 1] = hexArray[v & 0x0F];
                    }
                    final String hexStringValue = new String(hexChars);
                    jarTarget.addString("sha256", hexStringValue);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (dependency.sourcesUrl().toASCIIString().isEmpty()) {
                return Collections.singletonList(jarTarget);
            } else {
                final Target sourceTarget = new Target(dependency.mavenCoordinates(), "http_file", dependency.repositoryRuleName() + "__sources")
                        .addList("urls", Collections.singleton(dependency.sourcesUrl().toASCIIString()))
                        .addString("downloaded_file_path", getFilenameFromUrl(dependency.sourcesUrl().getPath()));

                return Arrays.asList(jarTarget, sourceTarget);
            }
        }
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
        public List<Target> buildTargets(Dependency dependency) {
            return targetsBuilders.stream().flatMap(builder -> builder.buildTargets(dependency).stream()).collect(Collectors.toList());
        }
    }

    public static class KotlinImport implements TargetsBuilder {

        @Override
        public List<Target> buildTargets(final Dependency dependency) {
            List<Target> targets = new ArrayList<>();

            targets.add(new Target(dependency.mavenCoordinates(), "kotlin_jar_support", dependency.repositoryRuleName())
                    .addList("deps", convertRulesToStrings(dependency.dependencies()))
                    .addList("exports", convertRulesToStrings(dependency.exports()))
                    .addList("runtime_deps", convertRulesToStrings(dependency.runtimeDependencies()))
                    .addString("jar", String.format(Locale.US, "@%s//file", dependency.repositoryRuleName()))
                    .addVariable("java_import_impl", "java_import_impl"));

            targets.add(addAlias(dependency));

            return targets;
        }
    }

    public static class JavaPluginFormatter implements TargetsBuilder {

        private static final String PROCESSOR_CLASS_POST_FIX = "___processor_class_";
        private static final String PLUGIN_WITH_API = "___generates_api";
        private static final String PROCESSOR_CLASS_POST_FIX_WITH_API = PLUGIN_WITH_API + PROCESSOR_CLASS_POST_FIX;
        private final List<String> processorClasses;

        JavaPluginFormatter(final Collection<String> processorClasses) {
            this.processorClasses = ImmutableList.copyOf(processorClasses);
        }

        @VisibleForTesting
        List<String> getProcessorClasses() {
            return processorClasses;
        }

        @Override
        public List<Target> buildTargets(Dependency dependency) {
            List<Target> targets = new ArrayList<>();

            //just as java-library
            targets.add(addJavaImportRule(dependency));
            targets.add(addAlias(dependency));

            Collection<String> deps = convertRulesToStrings(dependency.dependencies());
            deps.add(":" + dependency.repositoryRuleName());
            //as java_plugins
            List<String> noApiPlugins = new ArrayList<>();
            List<String> withApiPlugins = new ArrayList<>();
            for (int processorClassIndex = 0; processorClassIndex < processorClasses.size(); processorClassIndex++) {
                final String processorClass = processorClasses.get(processorClassIndex);

                final String noApiTargetName = dependency.repositoryRuleName() + PROCESSOR_CLASS_POST_FIX + processorClassIndex;
                noApiPlugins.add(":" + noApiTargetName);
                targets.add(new Target(dependency.mavenCoordinates(), "native.java_plugin", noApiTargetName)
                        .addString("processor_class", processorClass)
                        .addInt("generates_api", 0)
                        .addList("deps", deps));
                targets.add(addAlias(dependency, PROCESSOR_CLASS_POST_FIX + processorClassIndex));

                final String withApiTargetName = dependency.repositoryRuleName() + PROCESSOR_CLASS_POST_FIX_WITH_API + processorClassIndex;
                withApiPlugins.add(":" + withApiTargetName);
                targets.add(new Target(dependency.mavenCoordinates(), "native.java_plugin", withApiTargetName)
                        .addString("processor_class", processorClass)
                        .addInt("generates_api", 1)
                        .addList("deps", deps));
                targets.add(addAlias(dependency, PROCESSOR_CLASS_POST_FIX_WITH_API + processorClassIndex));
            }

            //collected java_plugins into a java_library.
            //If using those, then do not add them to the `plugins` list, but rather to the `deps`.
            targets.add(new Target(dependency.mavenCoordinates(),
                    "native.java_library",
                    dependency.repositoryRuleName() + PROCESSOR_CLASS_POST_FIX + "all")
                    .addList("exported_plugins", noApiPlugins));
            targets.add(addAlias(dependency, PROCESSOR_CLASS_POST_FIX + "all"));

            targets.add(new Target(dependency.mavenCoordinates(),
                    "native.java_library",
                    dependency.repositoryRuleName() + PROCESSOR_CLASS_POST_FIX_WITH_API + "all")
                    .addList("exported_plugins", withApiPlugins));
            targets.add(addAlias(dependency, PROCESSOR_CLASS_POST_FIX_WITH_API + "all"));

            return targets;
        }
    }

    static class AarImport implements TargetsBuilder {

        @Override
        public List<Target> buildTargets(final Dependency dependency) {
            List<Target> targets = new ArrayList<>();
            final Set<Dependency> deps = new HashSet<>(dependency.dependencies());
            deps.addAll(dependency.runtimeDependencies());

            targets.add(new Target(dependency.mavenCoordinates(), "aar_import_impl", dependency.repositoryRuleName())
                    .addString("aar", String.format(Locale.US, "@%s//file", dependency.repositoryRuleName()))
                    .addList("deps", convertRulesToStrings(deps))
                    .addList("exports", convertRulesToStrings(dependency.exports())));

            targets.add(addAlias(dependency));

            return targets;
        }
    }
}
