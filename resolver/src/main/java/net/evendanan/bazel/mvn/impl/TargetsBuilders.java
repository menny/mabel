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
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.TargetsBuilder;

public class TargetsBuilders {

    public static final TargetsBuilder JAVA_IMPORT = (dependency, dependencyTools) -> {
        List<Target> targets = new ArrayList<>();
        targets.add(addJavaImportRule(dependency, dependencyTools));
        targets.add(addAlias(dependency, dependencyTools));

        return targets;
    };
    static final TargetsBuilder KOTLIN_IMPORT = new KotlinImport();
    @VisibleForTesting
    static final TargetsBuilder AAR_IMPORT = new AarImport();

    private static Target addJavaImportRule(Dependency dependency, DependencyTools dependencyTools) {
        final Target target = new Target(dependencyTools.mavenCoordinates(dependency), "java_import_impl", dependencyTools.repositoryRuleName(dependency))
                .addList("jars", "pom".equalsIgnoreCase(dependency.getPackaging()) ?
                        Collections.emptyList()
                        :Collections.singletonList(String.format(Locale.ROOT, "@%s//file", dependencyTools.repositoryRuleName(dependency))))
                .addList("tags", Collections.singletonList(String.format(Locale.ROOT, "maven_coordinates=%s", dependencyTools.mavenCoordinates(dependency))))
                .addList("licenses", dependency.getLicensesList().stream().map(License::toString).collect(Collectors.toList()))
                .addList("deps", convertRulesToStrings(dependency.getDependenciesList(), dependencyTools))
                .addList("exports", convertRulesToStrings(dependency.getExportsList(), dependencyTools))
                .addList("runtime_deps", convertRulesToStrings(dependency.getRuntimeDependenciesList(), dependencyTools));
        if (!dependency.getSourcesUrl().isEmpty()) {
            target.addString("srcjar", String.format(Locale.US, "@%s__sources//file", dependencyTools.repositoryRuleName(dependency)));
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

    private static Collection<String> convertRulesToStrings(final Collection<Dependency> dependencies, DependencyTools dependencyTools) {
        return dependencies.stream()
                //using the friendly name (without version), so the mapping will be done via the alias mechanism.
                .map(dependencyTools::targetName)
                .map(name -> String.format(Locale.US, ":%s", name))
                .collect(Collectors.toList());
    }

    private static Target addAlias(Dependency dependency, DependencyTools dependencyTools) {
        return addAlias(dependency, "", dependencyTools);
    }

    private static Target addAlias(Dependency dependency, String postFix, DependencyTools dependencyTools) {
        return new Target(dependencyTools.mavenCoordinates(dependency), "native.alias", dependencyTools.targetName(dependency) + postFix)
                .addString("actual", String.format(Locale.US, ":%s%s", dependencyTools.repositoryRuleName(dependency), postFix))
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
        public List<Target> buildTargets(final Dependency dependency, DependencyTools dependencyTools) {
            if ("pom".equalsIgnoreCase(dependency.getPackaging())) return Collections.emptyList();

            final Target jarTarget = new Target(dependencyTools.mavenCoordinates(dependency), "http_file", dependencyTools.repositoryRuleName(dependency))
                    .addList("urls", Collections.singleton(dependency.getUrl()))
                    .addString("downloaded_file_path", getFilenameFromUrl(dependency.getUrl()));

            if (calculateSha && !dependency.getVersion().contains("SNAPSHOT")) {
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
            if (dependency.getSourcesUrl().isEmpty()) {
                return Collections.singletonList(jarTarget);
            } else {
                final Target sourceTarget = new Target(dependencyTools.mavenCoordinates(dependency), "http_file", dependencyTools.repositoryRuleName(dependency) + "__sources")
                        .addList("urls", Collections.singleton(dependency.getSourcesUrl()))
                        .addString("downloaded_file_path", getFilenameFromUrl(dependency.getSourcesUrl()));

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
        public List<Target> buildTargets(Dependency dependency, DependencyTools dependencyTools) {
            return targetsBuilders.stream().flatMap(builder -> builder.buildTargets(dependency, dependencyTools).stream()).collect(Collectors.toList());
        }
    }

    public static class KotlinImport implements TargetsBuilder {

        @Override
        public List<Target> buildTargets(final Dependency dependency, DependencyTools dependencyTools) {
            List<Target> targets = new ArrayList<>();

            targets.add(new Target(dependencyTools.mavenCoordinates(dependency), "kotlin_jar_support", dependencyTools.repositoryRuleName(dependency))
                    .addList("deps", convertRulesToStrings(dependency.getDependenciesList(), dependencyTools))
                    .addList("exports", convertRulesToStrings(dependency.getExportsList(), dependencyTools))
                    .addList("runtime_deps", convertRulesToStrings(dependency.getRuntimeDependenciesList(), dependencyTools))
                    .addString("jar", String.format(Locale.US, "@%s//file", dependencyTools.repositoryRuleName(dependency)))
                    .addVariable("java_import_impl", "java_import_impl"));

            targets.add(addAlias(dependency, dependencyTools));

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
        public List<Target> buildTargets(Dependency dependency, DependencyTools dependencyTools) {
            List<Target> targets = new ArrayList<>();

            //just as java-library
            targets.add(addJavaImportRule(dependency, dependencyTools));
            targets.add(addAlias(dependency, dependencyTools));

            Collection<String> deps = convertRulesToStrings(dependency.getDependenciesList(), dependencyTools);
            deps.add(":" + dependencyTools.repositoryRuleName(dependency));
            //as java_plugins
            List<String> noApiPlugins = new ArrayList<>();
            List<String> withApiPlugins = new ArrayList<>();
            for (int processorClassIndex = 0; processorClassIndex < processorClasses.size(); processorClassIndex++) {
                final String processorClass = processorClasses.get(processorClassIndex);

                final String noApiTargetName = dependencyTools.repositoryRuleName(dependency) + PROCESSOR_CLASS_POST_FIX + processorClassIndex;
                noApiPlugins.add(":" + noApiTargetName);
                targets.add(new Target(dependencyTools.mavenCoordinates(dependency), "native.java_plugin", noApiTargetName)
                        .addString("processor_class", processorClass)
                        .addInt("generates_api", 0)
                        .addList("deps", deps));
                targets.add(addAlias(dependency, PROCESSOR_CLASS_POST_FIX + processorClassIndex, dependencyTools));

                final String withApiTargetName = dependencyTools.repositoryRuleName(dependency) + PROCESSOR_CLASS_POST_FIX_WITH_API + processorClassIndex;
                withApiPlugins.add(":" + withApiTargetName);
                targets.add(new Target(dependencyTools.mavenCoordinates(dependency), "native.java_plugin", withApiTargetName)
                        .addString("processor_class", processorClass)
                        .addInt("generates_api", 1)
                        .addList("deps", deps));
                targets.add(addAlias(dependency, PROCESSOR_CLASS_POST_FIX_WITH_API + processorClassIndex, dependencyTools));
            }

            //collected java_plugins into a java_library.
            //If using those, then do not add them to the `plugins` list, but rather to the `deps`.
            targets.add(new Target(dependencyTools.mavenCoordinates(dependency),
                    "native.java_library",
                    dependencyTools.repositoryRuleName(dependency) + PROCESSOR_CLASS_POST_FIX + "all")
                    .addList("exported_plugins", noApiPlugins));
            targets.add(addAlias(dependency, PROCESSOR_CLASS_POST_FIX + "all", dependencyTools));

            targets.add(new Target(dependencyTools.mavenCoordinates(dependency),
                    "native.java_library",
                    dependencyTools.repositoryRuleName(dependency) + PROCESSOR_CLASS_POST_FIX_WITH_API + "all")
                    .addList("exported_plugins", withApiPlugins));
            targets.add(addAlias(dependency, PROCESSOR_CLASS_POST_FIX_WITH_API + "all", dependencyTools));

            return targets;
        }
    }

    static class AarImport implements TargetsBuilder {

        @Override
        public List<Target> buildTargets(final Dependency dependency, DependencyTools dependencyTools) {
            List<Target> targets = new ArrayList<>();
            final Set<Dependency> deps = new HashSet<>(dependency.getDependenciesList());
            deps.addAll(dependency.getRuntimeDependenciesList());

            targets.add(new Target(dependencyTools.mavenCoordinates(dependency), "aar_import_impl", dependencyTools.repositoryRuleName(dependency))
                    .addString("aar", String.format(Locale.US, "@%s//file", dependencyTools.repositoryRuleName(dependency)))
                    .addList("deps", convertRulesToStrings(deps, dependencyTools))
                    .addList("exports", convertRulesToStrings(dependency.getExportsList(), dependencyTools)));

            targets.add(addAlias(dependency, dependencyTools));

            return targets;
        }
    }
}
