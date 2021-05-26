package net.evendanan.bazel.mvn.impl;

import com.google.common.base.Charsets;

import net.evendanan.bazel.mvn.api.RuleClassifier;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuleClassifiers {

    public static TargetsBuilder priorityRuleClassifier(
            Collection<RuleClassifier> classifiers,
            TargetsBuilder defaultFormatter,
            final Dependency dependency) {
        return classifiers.stream()
                .map(classifier -> classifier.classifyRule(dependency))
                .filter(l -> !l.isEmpty())
                .findFirst()
                .<TargetsBuilder>map(TargetsBuilders.CompositeBuilder::new)
                .orElse(defaultFormatter);
    }

    private static boolean isNaivelyKotlin(Dependency dependency) {
        final Stream<MavenCoordinate> mvnStream =
                Stream.concat(
                        Stream.of(dependency.mavenCoordinate()),
                        dependency.dependencies().stream()
                );

        return mvnStream.anyMatch(m ->
                m.groupId().contains("jetbrains.kotlin") &&
                        (m.artifactId().contains("kotlin-stdlib") || m.artifactId().contains("kotlin-runtime")));
    }

    private static class PackagingClassifier implements RuleClassifier {

        private final String packaging;
        private final TargetsBuilder targetsBuilder;

        private PackagingClassifier(final String packaging, TargetsBuilder targetsBuilder) {
            this.packaging = packaging;
            this.targetsBuilder = targetsBuilder;
        }

        @Override
        public List<TargetsBuilder> classifyRule(final Dependency dependency) {
            if (packaging.equals(dependency.mavenCoordinate().packaging())) {
                return Collections.singletonList(targetsBuilder);
            } else {
                return Collections.emptyList();
            }
        }
    }

    public static class AarClassifier extends PackagingClassifier {
        public AarClassifier() {
            super("aar", TargetsBuilders.AAR_IMPORT);
        }
    }

    public static class NaiveKotlinClassifier implements RuleClassifier {
        @Override
        public List<TargetsBuilder> classifyRule(Dependency dependency) {
            if (isNaivelyKotlin(dependency)) {
                return Collections.singletonList(TargetsBuilders.KOTLIN_IMPORT);
            }
            return Collections.emptyList();
        }
    }

    public static class NaiveKotlinAarClassifier extends PackagingClassifier {
        public NaiveKotlinAarClassifier() {
            super("aar", TargetsBuilders.KOTLIN_ANDROID_IMPORT);
        }

        @Override
        public List<TargetsBuilder> classifyRule(Dependency dependency) {
            if (isNaivelyKotlin(dependency)) {
                return super.classifyRule(dependency);
            }
            return Collections.emptyList();
        }
    }

    public static class PomClassifier extends PackagingClassifier {
        public PomClassifier() {
            super("pom", TargetsBuilders.JAVA_IMPORT);
        }
    }

    public static class JarInspector {

        private final Function<Dependency, URI> downloader;

        public JarInspector(Function<Dependency, URI> downloader) {
            this.downloader = downloader;
        }

        private static List<TargetsBuilder> performRemoteJarInspection(InputStream inputStream)
                throws IOException {
            final List<TargetsBuilder> detectedModules = new ArrayList<>();
            try (JarInputStream zipInputStream = new JarInputStream(inputStream, false)) {
                JarEntry jarEntry = zipInputStream.getNextJarEntry();
                while (jarEntry != null) {
                    final String jarEntryName = jarEntry.getName();
                    if (jarEntryName.equalsIgnoreCase(
                            "META-INF/services/javax.annotation.processing.Processor")) {
                        StringBuilder contentBuilder = new StringBuilder();
                        final byte[] buffer = new byte[1024];
                        int read = 0;
                        while ((read = zipInputStream.read(buffer, 0, buffer.length)) >= 0) {
                            contentBuilder.append(new String(buffer, 0, read, Charsets.UTF_8));
                        }

                        parseServicesProcessorFileContent(contentBuilder.toString())
                                .ifPresent(detectedModules::add);
                    } else if (jarEntryName.startsWith("META-INF/")
                            && jarEntryName.endsWith(".kotlin_module")) {
                        detectedModules.add(TargetsBuilders.KOTLIN_IMPORT);
                    }
                    zipInputStream.closeEntry();
                    jarEntry = zipInputStream.getNextJarEntry();
                }
            }

            return detectedModules;
        }

        private static Optional<TargetsBuilder> parseServicesProcessorFileContent(
                String processorContent) {
            if (processorContent != null && processorContent.length() > 0) {
                final List<String> processors =
                        Arrays.stream(processorContent.split("\n", -1))
                                .filter(s -> s != null && s.length() > 0)
                                .filter(s -> !s.startsWith("#"))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());

                if (processors.size() > 0) {
                    return Optional.of(new TargetsBuilders.JavaPluginFormatter(processors));
                }
            }
            return Optional.empty();
        }

        public List<TargetsBuilder> findAllPossibleBuilders(final Dependency dependency) {
            try (InputStream networkInputStream =
                         downloader.apply(dependency).toURL().openStream()) {
                return performRemoteJarInspection(networkInputStream);
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }
    }

    public static class JarClassifier implements RuleClassifier {
        private final Function<Dependency, List<TargetsBuilder>> mJarInspector;

        public JarClassifier(Function<Dependency, List<TargetsBuilder>> jarInspector) {
            mJarInspector = jarInspector;
        }

        @Override
        public List<TargetsBuilder> classifyRule(Dependency dependency) {
            //TODO: in the future, we should use android import
            //final boolean isAndroid = dependency.url().endsWith(".aar");

            //we need to make sure the targets are ordered correctly:
            //  1) kotlin
            //  2) anything else
            List<TargetsBuilder> targetsBuilders = mJarInspector.apply(dependency);
            targetsBuilders.sort((o1, o2) -> {
                final boolean isKotlin1 = o1 instanceof TargetsBuilders.KotlinImport;
                final boolean isKotlin2 = o2 instanceof TargetsBuilders.KotlinImport;
                return isKotlin1 == isKotlin2 ? 0 : isKotlin1 ? -1 : 1;
            });
            return targetsBuilders;
        }
    }
}
