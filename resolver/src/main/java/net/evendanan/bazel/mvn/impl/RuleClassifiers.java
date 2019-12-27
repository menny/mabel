package net.evendanan.bazel.mvn.impl;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.RuleClassifier;
import net.evendanan.bazel.mvn.api.TargetsBuilder;

public class RuleClassifiers {

    public static TargetsBuilder priorityRuleClassifier(Collection<RuleClassifier> classifiers, TargetsBuilder defaultFormatter, final Dependency dependency) {
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
            if (packaging.equals(dependency.getPackaging())) {
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

    public static class JarInspector implements RuleClassifier {

        private final Function<Dependency, URI> downloader;

        public JarInspector(Function<Dependency, URI> downloader) {
            this.downloader = downloader;
        }

        private static Optional<TargetsBuilder> performRemoteJarInspection(InputStream inputStream) throws IOException {
            try (JarInputStream zipInputStream = new JarInputStream(inputStream, false)) {
                JarEntry jarEntry = zipInputStream.getNextJarEntry();
                while (jarEntry!=null) {
                    final String jarEntryName = jarEntry.getName();
                    if (jarEntryName.equalsIgnoreCase("META-INF/services/javax.annotation.processing.Processor")) {
                        StringBuilder contentBuilder = new StringBuilder();
                        final byte[] buffer = new byte[1024];
                        int read = 0;
                        while ((read = zipInputStream.read(buffer, 0, buffer.length)) >= 0) {
                            contentBuilder.append(new String(buffer, 0, read, Charsets.UTF_8));
                        }

                        return parseServicesProcessorFileContent(contentBuilder.toString());
                    } else if (jarEntryName.startsWith("META-INF/") && jarEntryName.endsWith(".kotlin_module")) {
                        return Optional.of(TargetsBuilders.KOTLIN_IMPORT);
                    }
                    zipInputStream.closeEntry();
                    jarEntry = zipInputStream.getNextJarEntry();
                }
            }

            return Optional.empty();
        }

        private static Optional<TargetsBuilder> parseServicesProcessorFileContent(String processorContent) {
            if (processorContent!=null && processorContent.length() > 0) {
                final List<String> processors = Arrays.stream(processorContent.split("\n", -1))
                        .filter(s -> s!=null && s.length() > 0)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

                if (processors.size() > 0) {
                    return Optional.of(new TargetsBuilders.JavaPluginFormatter(processors));
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<TargetsBuilder> classifyRule(final Dependency dependency) {
            try (InputStream networkInputStream = downloader.apply(dependency).toURL().openStream()) {
                return performRemoteJarInspection(networkInputStream);
            } catch (IOException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }
    }
}
