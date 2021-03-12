package net.evendanan.bazel.mvn.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.LicenseTools;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.License;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;

import org.apache.commons.lang3.StringUtils;

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
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TargetsBuilders {

    public static final TargetsBuilder JAVA_IMPORT =
            (dependency, dependencyTools) -> {
                List<Target> targets = new ArrayList<>();
                targets.add(addJavaImportRule(dependency, dependencyTools));
                targets.add(
                        addAlias(
                                dependency,
                                dependency.mavenCoordinate().artifactId(),
                                "",
                                dependencyTools));

                return targets;
            };
    static final TargetsBuilder KOTLIN_IMPORT = new KotlinImport();
    static final TargetsBuilder KOTLIN_ANDROID_IMPORT = new KotlinAndroidImport();
    @VisibleForTesting
    static final TargetsBuilder AAR_IMPORT = new AarImport();

    private static Target addJavaImportRule(
            Dependency dependency, DependencyTools dependencyTools) {
        final Target target =
                new Target(
                        dependencyTools.mavenCoordinates(dependency),
                        "java_import",
                        dependencyTools.repositoryRuleName(dependency))
                        .addList(
                                "jars",
                                "pom".equalsIgnoreCase(dependency.mavenCoordinate().packaging())
                                        ? Collections.emptyList()
                                        : Collections.singletonList(
                                        String.format(
                                                Locale.ROOT,
                                                "@%s//file",
                                                dependencyTools.repositoryRuleName(
                                                        dependency))))
                        .addList("tags", dependencyTagsList(dependency, dependencyTools))
                        .addList(
                                "licenses",
                                dependency.licenses().stream()
                                        .map(License::name)
                                        .map(LicenseTools::classFromLicenseName)
                                        .filter(Objects::nonNull)
                                        .map(Object::toString)
                                        .collect(Collectors.toList()))
                        .addList(
                                "deps",
                                convertRulesToStrings(dependency.dependencies(), dependencyTools))
                        .addList(
                                "exports",
                                convertRulesToStrings(dependency.exports(), dependencyTools))
                        .addList(
                                "runtime_deps",
                                convertRulesToStrings(
                                        dependency.runtimeDependencies(), dependencyTools));
        if (!dependency.sourcesUrl().isEmpty()) {
            target.addString(
                    "srcjar",
                    String.format(
                            Locale.US,
                            "@%s__sources//file",
                            dependencyTools.repositoryRuleName(dependency)));
        }
        return target;
    }

    private static Collection<String> dependencyTagsList(Dependency dependency, DependencyTools dependencyTools) {
        List<String> tags = new ArrayList<>();
        tags.add(String.format(
                Locale.ROOT,
                "maven_coordinates=%s",
                dependencyTools.mavenCoordinates(dependency)));

        dependency.licenses().forEach(l -> {
            if (!StringUtils.isBlank(l.name())) {
                tags.add(String.format(Locale.ROOT, "mabel_license_name=%s", escapeText(l.name())));
                tags.add(String.format(Locale.ROOT, "mabel_license_detected_type=%s", LicenseTools.typeFromLicenseName(l.name())));
                if (!StringUtils.isBlank(l.url())) {
                    tags.add(String.format(Locale.ROOT, "mabel_license_url=%s", l.url()));
                }
            }
        });

        return tags;
    }

    private static String escapeText(String text) {
        return text
                .replace("\"", "\\\"")
                .replace('\r', ' ')
                .replace('\n', ' ');
    }

    private static String getFilenameFromUrl(String url) {
        int lastPathSeparator = url.lastIndexOf("/");
        if (lastPathSeparator < 0) {
            throw new IllegalArgumentException("Could not parse filename out of URL '" + url + "'");
        }

        return url.substring(lastPathSeparator + 1);
    }

    private static Collection<String> convertRulesToStrings(
            final Collection<MavenCoordinate> dependencies, DependencyTools dependencyTools) {
        return dependencies.stream()
                // using the friendly name (without version), so the mapping will be done via the
                // alias mechanism.
                .map(dependencyTools::targetName)
                .map(name -> String.format(Locale.US, ":%s", name))
                .collect(Collectors.toList());
    }

    private static Target addAlias(Dependency dependency, DependencyTools dependencyTools) {
        return addAlias(dependency, "", dependencyTools);
    }

    private static Target addAlias(
            Dependency dependency, String postFix, DependencyTools dependencyTools) {
        return addAlias(
                dependency,
                String.format(Locale.ROOT, "%s%s", dependencyTools.targetName(dependency), postFix),
                postFix,
                dependencyTools);
    }

    private static Target addAlias(
            Dependency dependency,
            String nameSpaced,
            String postFix,
            DependencyTools dependencyTools) {
        return new Target(
                dependencyTools.mavenCoordinates(dependency),
                "native.alias",
                String.format(
                        Locale.ROOT,
                        "%s%s",
                        dependencyTools.targetName(dependency),
                        postFix),
                nameSpaced)
                .addString(
                        "actual",
                        String.format(
                                Locale.ROOT,
                                ":%s%s",
                                dependencyTools.repositoryRuleName(dependency),
                                postFix))
                .setPublicVisibility();
    }

    private static <T> Collection<T> addItem(Collection<T> list, T item) {
        List<T> newList = new ArrayList<>(list);
        newList.add(item);
        return newList;
    }

    public static class HttpTargetsBuilder implements TargetsBuilder {
        private static final char[] hexArray = "0123456789abcdef".toCharArray();
        private final boolean calculateSha;
        private final byte[] readBuffer;
        private final Function<Dependency, URI> downloader;

        public HttpTargetsBuilder(boolean calculateSha, Function<Dependency, URI> downloader) {
            this.calculateSha = calculateSha;
            this.readBuffer = calculateSha ? new byte[4096] : new byte[0];
            this.downloader = downloader;
        }

        @Override
        public List<Target> buildTargets(
                final Dependency dependency, DependencyTools dependencyTools) {
            if ("pom".equalsIgnoreCase(dependency.mavenCoordinate().packaging()))
                return Collections.emptyList();

            final Target jarTarget =
                    new Target(
                            dependencyTools.mavenCoordinates(dependency),
                            "http_file",
                            dependencyTools.repositoryRuleName(dependency))
                            .addList("urls", Collections.singleton(dependency.url()))
                            .addString(
                                    "downloaded_file_path", getFilenameFromUrl(dependency.url()));

            if (calculateSha && !dependency.mavenCoordinate().version().contains("SNAPSHOT")) {
                try (InputStream inputStream = downloader.apply(dependency).toURL().openStream()) {
                    final MessageDigest digest = MessageDigest.getInstance("SHA-256");

                    int bytesCount;
                    while ((bytesCount = inputStream.read(readBuffer)) != -1) {
                        digest.update(readBuffer, 0, bytesCount);
                    }

                    byte[] digestBytes = digest.digest();
                    char[] hexChars = new char[digestBytes.length * 2];
                    for (int digestByteIndex = 0;
                         digestByteIndex < digestBytes.length;
                         digestByteIndex++) {
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
            if (dependency.sourcesUrl().isEmpty()) {
                return Collections.singletonList(jarTarget);
            } else {
                final Target sourceTarget =
                        new Target(
                                dependencyTools.mavenCoordinates(dependency),
                                "http_file",
                                dependencyTools.repositoryRuleName(dependency)
                                        + "__sources")
                                .addList("urls", Collections.singleton(dependency.sourcesUrl()))
                                .addString(
                                        "downloaded_file_path",
                                        getFilenameFromUrl(dependency.sourcesUrl()));

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
            Set<String> seenTargets = new HashSet<>();
            return targetsBuilders.stream()
                    .flatMap(builder -> builder.buildTargets(dependency, dependencyTools).stream())
                    .filter(t -> seenTargets.add(t.getTargetName()))
                    .collect(Collectors.toList());
        }

        @VisibleForTesting
        public Collection<TargetsBuilder> getTargetsBuilders() {
            return targetsBuilders;
        }
    }

    public static class KotlinImport implements TargetsBuilder {

        @Override
        public List<Target> buildTargets(
                final Dependency dependency, DependencyTools dependencyTools) {
            List<Target> targets = new ArrayList<>();

            //will create a kt_import for the jar
            //and another kt_library for the jar+deps
            //the kt_library is the visible one
            final String ktTargetName = getKotlinJvmRepositoryRuleName(dependency, dependencyTools);
            final String ktTargetImportName = ktTargetName + "_kt_jvm_import";
            targets.add(
                    new Target(
                            dependencyTools.mavenCoordinates(dependency),
                            "kt_jvm_import",
                            ktTargetImportName)
                            .addString(
                                    "jar",
                                    String.format(
                                            Locale.US,
                                            "@%s//file",
                                            dependencyTools.repositoryRuleName(dependency)))
                            .setPrivateVisibility());
            targets.add(
                    new Target(
                            dependencyTools.mavenCoordinates(dependency),
                            "kt_jvm_library",
                            ktTargetName)
                            .addList(
                                    "tags",
                                    Collections.singletonList(
                                            String.format(
                                                    Locale.ROOT,
                                                    "maven_coordinates=%s",
                                                    dependencyTools.mavenCoordinates(dependency))))
                            .addList(
                                    "exports",
                                    addItem(convertRulesToStrings(dependency.exports(), dependencyTools), ":" + ktTargetImportName))
                            .addList(
                                    "runtime_deps",
                                    convertRulesToStrings(
                                            dependency.runtimeDependencies(), dependencyTools)));

            targets.add(
                    addAlias(
                            dependency,
                            dependency.mavenCoordinate().artifactId(),
                            "",
                            dependencyTools));

            return targets;
        }

        protected String getKotlinJvmRepositoryRuleName(Dependency dependency, DependencyTools dependencyTools) {
            return dependencyTools.repositoryRuleName(dependency);
        }
    }

    public static class KotlinAndroidImport extends KotlinImport {

        @Override
        protected String getKotlinJvmRepositoryRuleName(Dependency dependency, DependencyTools dependencyTools) {
            return super.getKotlinJvmRepositoryRuleName(dependency, dependencyTools) + "___kt_library";
        }

        @Override
        public List<Target> buildTargets(
                final Dependency dependency, DependencyTools dependencyTools) {
            List<Target> targets = super.buildTargets(dependency, dependencyTools);

            targets.add(
                    new Target(
                            dependencyTools.mavenCoordinates(dependency),
                            "kt_android_library",
                            dependencyTools.repositoryRuleName(dependency))
                            .addList(
                                    "deps",
                                    convertRulesToStrings(dependency.dependencies(), dependencyTools))
                            .addList(
                                    "exports",
                                    addItem(convertRulesToStrings(dependency.exports(), dependencyTools), ":" + getKotlinJvmRepositoryRuleName(dependency, dependencyTools)))
                            .addList(
                                    "tags",
                                    Collections.singletonList(
                                            String.format(
                                                    Locale.ROOT,
                                                    "maven_coordinates=%s",
                                                    dependencyTools.mavenCoordinates(dependency)))));

            return targets;
        }
    }

    public static class JavaPluginFormatter implements TargetsBuilder {

        private static final String PROCESSOR_CLASS_POST_FIX = "___processor_class_";
        private static final String PLUGIN_WITH_API = "___generates_api";
        private static final String PROCESSOR_CLASS_POST_FIX_WITH_API =
                PLUGIN_WITH_API + PROCESSOR_CLASS_POST_FIX;
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

            // just as java-library
            targets.add(addJavaImportRule(dependency, dependencyTools));
            targets.add(
                    addAlias(
                            dependency,
                            dependency.mavenCoordinate().artifactId(),
                            "",
                            dependencyTools));

            Collection<String> deps =
                    convertRulesToStrings(dependency.dependencies(), dependencyTools);
            deps.add(":" + dependencyTools.repositoryRuleName(dependency));
            // as java_plugins
            List<String> noApiPlugins = new ArrayList<>();
            List<String> withApiPlugins = new ArrayList<>();
            for (int processorClassIndex = 0;
                 processorClassIndex < processorClasses.size();
                 processorClassIndex++) {
                final String processorClass = processorClasses.get(processorClassIndex);

                final String noApiTargetName =
                        dependencyTools.repositoryRuleName(dependency)
                                + PROCESSOR_CLASS_POST_FIX
                                + processorClassIndex;
                noApiPlugins.add(":" + noApiTargetName);
                targets.add(
                        new Target(
                                dependencyTools.mavenCoordinates(dependency),
                                "java_plugin",
                                noApiTargetName)
                                .addString("processor_class", processorClass)
                                .addInt("generates_api", 0)
                                .addList("deps", deps));
                targets.add(
                        addAlias(
                                dependency,
                                PROCESSOR_CLASS_POST_FIX + processorClassIndex,
                                dependencyTools));

                final String withApiTargetName =
                        dependencyTools.repositoryRuleName(dependency)
                                + PROCESSOR_CLASS_POST_FIX_WITH_API
                                + processorClassIndex;
                withApiPlugins.add(":" + withApiTargetName);
                targets.add(
                        new Target(
                                dependencyTools.mavenCoordinates(dependency),
                                "java_plugin",
                                withApiTargetName)
                                .addString("processor_class", processorClass)
                                .addInt("generates_api", 1)
                                .addList("deps", deps));
                targets.add(
                        addAlias(
                                dependency,
                                PROCESSOR_CLASS_POST_FIX_WITH_API + processorClassIndex,
                                dependencyTools));
            }

            // collected java_plugins into a java_library.
            // If using those, then do not add them to the `plugins` list, but rather to the `deps`.
            targets.add(
                    new Target(
                            dependencyTools.mavenCoordinates(dependency),
                            "java_library",
                            dependencyTools.repositoryRuleName(dependency)
                                    + PROCESSOR_CLASS_POST_FIX
                                    + "all")
                            .addList("exported_plugins", noApiPlugins));
            targets.add(
                    addAlias(
                            dependency,
                            "processors",
                            PROCESSOR_CLASS_POST_FIX + "all",
                            dependencyTools));

            targets.add(
                    new Target(
                            dependencyTools.mavenCoordinates(dependency),
                            "java_library",
                            dependencyTools.repositoryRuleName(dependency)
                                    + PROCESSOR_CLASS_POST_FIX_WITH_API
                                    + "all")
                            .addList("exported_plugins", withApiPlugins));
            targets.add(
                    addAlias(
                            dependency,
                            "processors_with_api",
                            PROCESSOR_CLASS_POST_FIX_WITH_API + "all",
                            dependencyTools));

            return targets;
        }
    }

    static class AarImport implements TargetsBuilder {

        @Override
        public List<Target> buildTargets(
                final Dependency dependency, DependencyTools dependencyTools) {
            List<Target> targets = new ArrayList<>();
            final Set<MavenCoordinate> deps = new HashSet<>(dependency.dependencies());
            deps.addAll(dependency.runtimeDependencies());

            targets.add(
                    new Target(
                            dependencyTools.mavenCoordinates(dependency),
                            "aar_import",
                            dependencyTools.repositoryRuleName(dependency))
                            .addString(
                                    "aar",
                                    String.format(
                                            Locale.US,
                                            "@%s//file",
                                            dependencyTools.repositoryRuleName(dependency)))
                            .addList(
                                    "tags",
                                    Collections.singletonList(
                                            String.format(
                                                    Locale.ROOT,
                                                    "maven_coordinates=%s",
                                                    dependencyTools.mavenCoordinates(dependency))))
                            .addList("deps", convertRulesToStrings(deps, dependencyTools))
                            .addList(
                                    "exports",
                                    convertRulesToStrings(dependency.exports(), dependencyTools)));

            targets.add(
                    addAlias(
                            dependency,
                            dependency.mavenCoordinate().artifactId(),
                            "",
                            dependencyTools));

            return targets;
        }
    }
}
