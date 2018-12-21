package net.evendanan.bazel.mvn;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.devtools.bazel.workspace.maven.Rule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuleClassifiers {

    public static RuleFormatter ruleClassifier(final Rule rule) {
        return Stream.of(
            RuleClassifiers.AAR_IMPORT,
            RuleClassifiers.JAR_INSPECTOR
        )
            .map(classifier -> classifier.classifyRule(rule))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElse(RuleFormatters.JAVA_IMPORT);
    }

    @VisibleForTesting
    static final RuleClassifier AAR_IMPORT = rule -> {
        if ("aar".equals(rule.packaging())) {
            return Optional.of(RuleFormatters.AAR_IMPORT);
        } else {
            return Optional.empty();
        }
    };

    private static final RuleClassifier JAR_INSPECTOR = rule -> {
        try (InputStream networkInputStream = new URL(rule.getUrl()).openStream()) {
            return performRemoteJarInspection(networkInputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    };

    @VisibleForTesting
    static Optional<RuleFormatter> performRemoteJarInspection(final InputStream networkInputStream) throws IOException {
        try (JarInputStream zipInputStream = new JarInputStream(networkInputStream, false)) {
            JarEntry jarEntry = zipInputStream.getNextJarEntry();
            while (jarEntry != null) {
                if (jarEntry.getName().equalsIgnoreCase("META-INF/services/javax.annotation.processing.Processor")) {
                    StringBuilder contentBuilder = new StringBuilder();
                    final byte[] buffer = new byte[1024];
                    int read = 0;
                    while ((read = zipInputStream.read(buffer, 0, buffer.length)) >= 0) {
                        contentBuilder.append(new String(buffer, 0, read, Charsets.UTF_8));
                    }

                    return parseServicesProcessorFileContent(contentBuilder.toString());
                }
                zipInputStream.closeEntry();
                jarEntry = zipInputStream.getNextJarEntry();
            }
        }

        return Optional.empty();
    }

    private static Optional<RuleFormatter> parseServicesProcessorFileContent(final String processorContent) {
        if (processorContent != null && processorContent.length() > 0) {
            final List<RuleFormatter> processors = Arrays.stream(processorContent.split("\n", -1))
                .filter(s -> s != null && s.length() > 0)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(RuleClassifiers::createProcessorFormatter)
                .collect(Collectors.toList());

            if (processors.size() > 0) {
                return Optional.of(new RuleFormatters.CompositeFormatter(processors));
            }
        }
        return Optional.empty();
    }

    private static RuleFormatter createProcessorFormatter(final String processorClassName) {
        return new RuleFormatters.JavaPluginFormatter(processorClassName);
    }
}
