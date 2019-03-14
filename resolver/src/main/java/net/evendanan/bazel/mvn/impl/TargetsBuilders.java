package net.evendanan.bazel.mvn.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.License;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.TargetsBuilder;

public class TargetsBuilders {

    @VisibleForTesting
    static class HttpTargetsBuilder implements TargetsBuilder {
        private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
        private final boolean calculateSha;
        private final byte[] readBuffer;

        HttpTargetsBuilder(boolean calculateSha) {
            this.calculateSha = calculateSha;
            this.readBuffer = calculateSha? new byte[4096] : new byte[0];
        }

        @Override
        public List<Target> buildTargets(final Dependency dependency) {
            if ("pom".equalsIgnoreCase(dependency.packaging())) return Collections.emptyList();

            final Target jarTarget = new Target(dependency.mavenCoordinates(), "http_file", dependency.repositoryRuleName())
                    .addList("urls", Collections.singleton(dependency.url().toASCIIString()))
                    .addString("downloaded_file_path", getFilenameFromUrl(dependency.url().getPath()));

            if (calculateSha) {
                try (InputStream inputStream = inputStreamForUrl(dependency.url())) {
                    final MessageDigest digest = MessageDigest.getInstance("SHA-256");

                    int bytesCount;
                    while ((bytesCount = inputStream.read(readBuffer)) != -1) {
                        digest.update(readBuffer, 0, bytesCount);
                    }

                    byte[] digestBytes = digest.digest();
                    char[] hexChars = new char[digestBytes.length * 2];
                    for ( int digestByteIndex = 0; digestByteIndex < digestBytes.length; digestByteIndex++ ) {
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

        @VisibleForTesting
        InputStream inputStreamForUrl(final URI url) throws Exception {
            return url.toURL().openStream();
        }
    }
    public static final TargetsBuilder HTTP_FILE = new HttpTargetsBuilder(false);
    public static final TargetsBuilder HTTP_FILE_WITH_SHA = new HttpTargetsBuilder(true);

    static final TargetsBuilder JAVA_IMPORT = dependency -> {
        List<Target> targets = new ArrayList<>();
        targets.add(addJavaImportRule(false, dependency, ""));
        targets.add(addAlias(false, dependency));

        return targets;
    };
    static final TargetsBuilder NATIVE_JAVA_IMPORT = dependency -> {
        List<Target> targets = new ArrayList<>();
        targets.add(addJavaImportRule(true, dependency, ""));
        targets.add(addAlias(true, dependency));

        return targets;
    };
    static final TargetsBuilder KOTLIN_IMPORT = new KotlinImport(false);
    static final TargetsBuilder NATIVE_KOTLIN_IMPORT = new KotlinImport(true);
    @VisibleForTesting
    static final TargetsBuilder AAR_IMPORT = new AarImport(false);
    @VisibleForTesting
    static final TargetsBuilder NATIVE_AAR_IMPORT = new AarImport(true);

    private static Target addJavaImportRule(boolean asNative, Dependency dependency, String postFix) {
        final Target target = new Target(dependency.mavenCoordinates(), asNative ? "native.java_import":"java_import", dependency.repositoryRuleName() + postFix)
                .addList("jars", "pom".equalsIgnoreCase(dependency.packaging()) ?
                        Collections.emptyList()
                        :Collections.singletonList(String.format(Locale.US, "@%s//file", dependency.repositoryRuleName())))
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

    private static Target addAlias(boolean asNative, Dependency dependency) {
        return addAlias(asNative, dependency, "");
    }

    private static Target addAlias(boolean asNative, Dependency dependency, String postFix) {
        return new Target(dependency.mavenCoordinates(), asNative ? "native.alias":"alias", dependency.targetName() + postFix)
                .addString("actual", String.format(Locale.US, ":%s%s", dependency.repositoryRuleName(), postFix))
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
        public List<Target> buildTargets(Dependency dependency) {
            return targetsBuilders.stream().flatMap(builder -> builder.buildTargets(dependency).stream()).collect(Collectors.toList());
        }
    }

    public static class KotlinImport implements TargetsBuilder {

        private final boolean asNative;

        KotlinImport(final boolean asNative) {
            this.asNative = asNative;
        }

        @Override
        public List<Target> buildTargets(final Dependency dependency) {
            List<Target> targets = new ArrayList<>();

            targets.add(new Target(dependency.mavenCoordinates(), "kotlin_jar_support", dependency.repositoryRuleName())
                    .addList("deps", convertRulesToStrings(dependency.dependencies()))
                    .addList("exports", convertRulesToStrings(dependency.exports()))
                    .addList("runtime_deps", convertRulesToStrings(dependency.runtimeDependencies()))
                    .addString("jar", String.format(Locale.US, "@%s//file", dependency.repositoryRuleName())));

            targets.add(addAlias(asNative, dependency));

            return targets;
        }
    }

    public static class JavaPluginFormatter implements TargetsBuilder {

        private static final String PROCESSOR_CLASS_POST_FIX = "___processor_class_";
        private static final String PLUGIN_WITH_API = "___generates_api";
        private static final String PROCESSOR_CLASS_POST_FIX_WITH_API = PLUGIN_WITH_API + PROCESSOR_CLASS_POST_FIX;
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
        public List<Target> buildTargets(Dependency dependency) {
            List<Target> targets = new ArrayList<>();

            //just as java-library
            targets.add(addJavaImportRule(asNative, dependency, ""));
            targets.add(addAlias(asNative, dependency));

            Collection<String> deps = convertRulesToStrings(dependency.dependencies());
            deps.add(":" + dependency.repositoryRuleName());
            //as java_plugins
            List<String> noApiPlugins = new ArrayList<>();
            List<String> withApiPlugins = new ArrayList<>();
            for (int processorClassIndex = 0; processorClassIndex < processorClasses.size(); processorClassIndex++) {
                final String processorClass = processorClasses.get(processorClassIndex);

                final String noApiTargetName = dependency.repositoryRuleName() + PROCESSOR_CLASS_POST_FIX + processorClassIndex;
                noApiPlugins.add(":" + noApiTargetName);
                targets.add(new Target(dependency.mavenCoordinates(), asNative ? "native.java_plugin":"java_plugin", noApiTargetName)
                        .addString("processor_class", processorClass)
                        .addInt("generates_api", 0)
                        .addList("deps", deps));
                targets.add(addAlias(asNative, dependency, PROCESSOR_CLASS_POST_FIX + processorClassIndex));

                final String withApiTargetName = dependency.repositoryRuleName() + PROCESSOR_CLASS_POST_FIX_WITH_API + processorClassIndex;
                withApiPlugins.add(":" + withApiTargetName);
                targets.add(new Target(dependency.mavenCoordinates(), asNative ? "native.java_plugin":"java_plugin", withApiTargetName)
                        .addString("processor_class", processorClass)
                        .addInt("generates_api", 1)
                        .addList("deps", deps));
                targets.add(addAlias(asNative, dependency, PROCESSOR_CLASS_POST_FIX_WITH_API + processorClassIndex));
            }

            //collected java_plugins into a java_library.
            //If using those, then do not add them to the `plugins` list, but rather to the `deps`.
            targets.add(new Target(dependency.mavenCoordinates(),
                    asNative ? "native.java_library":"java_library",
                    dependency.repositoryRuleName() + PROCESSOR_CLASS_POST_FIX + "all")
                    .addList("exported_plugins", noApiPlugins));
            targets.add(addAlias(asNative, dependency, PROCESSOR_CLASS_POST_FIX + "all"));

            targets.add(new Target(dependency.mavenCoordinates(),
                    asNative ? "native.java_library":"java_library",
                    dependency.repositoryRuleName() + PROCESSOR_CLASS_POST_FIX_WITH_API + "all")
                    .addList("exported_plugins", withApiPlugins));
            targets.add(addAlias(asNative, dependency, PROCESSOR_CLASS_POST_FIX_WITH_API + "all"));

            return targets;
        }
    }

    static class AarImport implements TargetsBuilder {

        private final boolean asNative;

        AarImport(final boolean asNative) {
            this.asNative = asNative;
        }

        @Override
        public List<Target> buildTargets(final Dependency dependency) {
            List<Target> targets = new ArrayList<>();
            final Set<Dependency> deps = new HashSet<>(dependency.dependencies());
            deps.addAll(dependency.runtimeDependencies());

            targets.add(new Target(dependency.mavenCoordinates(), asNative ? "native.aar_import":"aar_import", dependency.repositoryRuleName())
                    .addString("aar", String.format(Locale.US, "@%s//file", dependency.repositoryRuleName()))
                    .addList("deps", convertRulesToStrings(deps))
                    .addList("exports", convertRulesToStrings(dependency.exports())));

            targets.add(addAlias(asNative, dependency));

            return targets;
        }
    }
}
