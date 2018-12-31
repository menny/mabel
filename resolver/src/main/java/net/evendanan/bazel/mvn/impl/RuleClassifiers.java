package net.evendanan.bazel.mvn.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.devtools.bazel.workspace.maven.Rule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.RuleClassifier;
import net.evendanan.bazel.mvn.api.TargetsBuilder;

public class RuleClassifiers {

    static final RuleClassifier AAR_IMPORT = new AarClassifier(false);
    static final RuleClassifier NATIVE_AAR_IMPORT = new AarClassifier(true);
    private static final RuleClassifier JAR_INSPECTOR = new JarInspector(false);
    public static final Function<Rule, TargetsBuilder> NONE_NATIVE_RULE_MAPPER = new Function<Rule, TargetsBuilder>() {
        @Override
        public TargetsBuilder apply(final Rule rule) {
            return ruleClassifier(Arrays.asList(AAR_IMPORT, JAR_INSPECTOR), TargetsBuilders.JAVA_IMPORT, rule);
        }
    };
    private static final RuleClassifier NATIVE_JAR_INSPECTOR = new JarInspector(true);
    public static final Function<Rule, TargetsBuilder> NATIVE_RULE_MAPPER = new Function<Rule, TargetsBuilder>() {
        @Override
        public TargetsBuilder apply(final Rule rule) {
            return ruleClassifier(Arrays.asList(NATIVE_AAR_IMPORT, NATIVE_JAR_INSPECTOR), TargetsBuilders.NATIVE_JAVA_IMPORT, rule);
        }
    };

    private static TargetsBuilder ruleClassifier(Collection<RuleClassifier> classifiers, TargetsBuilder defaultFormatter, final Rule rule) {
        return classifiers.stream()
                .map(classifier -> classifier.classifyRule(rule))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(defaultFormatter);
    }

    @VisibleForTesting
    static Optional<TargetsBuilder> performRemoteJarInspection(boolean asNative, InputStream networkInputStream) throws IOException {
        try (JarInputStream zipInputStream = new JarInputStream(networkInputStream, false)) {
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

                    return parseServicesProcessorFileContent(asNative, contentBuilder.toString());
                } else if (jarEntryName.startsWith("META-INF/") && jarEntryName.endsWith(".kotlin_module")) {
                    //I don't really understand how to import Kotlin JARs, but it seems that
                    //regular java_import works fine. So, I'll go with that for now.
                    //return Optional.of(asNative ? TargetsBuilders.NATIVE_KOTLIN_IMPORT : TargetsBuilders.KOTLIN_IMPORT);
                    return Optional.of(asNative ? TargetsBuilders.NATIVE_JAVA_IMPORT:TargetsBuilders.JAVA_IMPORT);
                }
                zipInputStream.closeEntry();
                jarEntry = zipInputStream.getNextJarEntry();
            }
        }

        return Optional.empty();
    }

    private static Optional<TargetsBuilder> parseServicesProcessorFileContent(boolean asNative, String processorContent) {
        if (processorContent!=null && processorContent.length() > 0) {
            final List<String> processors = Arrays.stream(processorContent.split("\n", -1))
                    .filter(s -> s!=null && s.length() > 0)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (processors.size() > 0) {
                return Optional.of(new TargetsBuilders.JavaPluginFormatter(asNative, processors));
            }
        }
        return Optional.empty();
    }

    private static class AarClassifier implements RuleClassifier {

        private final boolean asNative;

        private AarClassifier(final boolean asNative) {
            this.asNative = asNative;
        }

        @Override
        public Optional<TargetsBuilder> classifyRule(final Rule rule) {
            if ("aar".equals(rule.packaging())) {
                return Optional.of(asNative ? TargetsBuilders.NATIVE_AAR_IMPORT:TargetsBuilders.AAR_IMPORT);
            } else {
                return Optional.empty();
            }
        }
    }

    private static class JarInspector implements RuleClassifier {

        private final boolean asNative;

        private JarInspector(final boolean asNative) {
            this.asNative = asNative;
        }

        @Override
        public Optional<TargetsBuilder> classifyRule(final Rule rule) {
            try (InputStream networkInputStream = new URL(rule.getUrl()).openStream()) {
                return performRemoteJarInspection(asNative, networkInputStream);
            } catch (IOException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }
    }
}
