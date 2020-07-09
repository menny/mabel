package net.evendanan.bazel.mvn.impl;

import com.google.common.base.Charsets;

import net.evendanan.bazel.mvn.api.RuleClassifier;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import net.evendanan.bazel.mvn.api.model.Dependency;

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

public class RuleClassifiers {

    public static TargetsBuilder priorityRuleClassifier(
            Collection<RuleClassifier> classifiers,
            TargetsBuilder defaultFormatter,
            final Dependency dependency) {
        return classifiers.stream()
                .map(classifier -> classifier.classifyRule(dependency))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(defaultFormatter);
    }

    private static class PackagingClassifier implements RuleClassifier {

        private final String packaging;
        private final TargetsBuilder targetsBuilder;

        private PackagingClassifier(final String packaging, TargetsBuilder targetsBuilder) {
            this.packaging = packaging;
            this.targetsBuilder = targetsBuilder;
        }

        @Override
        public Optional<TargetsBuilder> classifyRule(final Dependency dependency) {
            if (packaging.equals(dependency.mavenCoordinate().packaging())) {
                return Optional.of(targetsBuilder);
            } else {
                return Optional.empty();
            }
        }
    }

    public static class AarClassifier extends PackagingClassifier {
        public AarClassifier() {
            super("aar", TargetsBuilders.AAR_IMPORT);
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
        private Function<Dependency, List<TargetsBuilder>> mJarInspector;

        public JarClassifier(Function<Dependency, List<TargetsBuilder>> jarInspector) {
            mJarInspector = jarInspector;
        }

        @Override
        public Optional<TargetsBuilder> classifyRule(Dependency dependency) {
            //TODO: in the future, we should use android import
            //final boolean isAndroid = dependency.url().endsWith(".aar");
            List<TargetsBuilder> possibleBuilders = mJarInspector.apply(dependency);
            final Optional<TargetsBuilder> kotlinOptional = possibleBuilders.stream().filter(t -> t instanceof TargetsBuilders.KotlinImport).findFirst();
            final Optional<TargetsBuilder> processorOptional = possibleBuilders.stream().filter(t -> t instanceof TargetsBuilders.JavaPluginFormatter).findFirst();

            if (kotlinOptional.isPresent()) {
                return kotlinOptional;
            } else {
                //either this is present - in which case we want to return it - or it's empty - in which case want to return empty.
                return processorOptional;
            }
        }
    }
}
