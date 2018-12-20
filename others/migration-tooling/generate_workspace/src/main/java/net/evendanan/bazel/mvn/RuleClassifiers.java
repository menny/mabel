package net.evendanan.bazel.mvn;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
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

    private static final RuleClassifier AAR_IMPORT = rule -> {
        if ("aar".equals(rule.packaging())) {
            return Optional.of(RuleFormatters.AAR_IMPORT);
        } else {
            return Optional.empty();
        }
    };

    private static final RuleClassifier JAR_INSPECTOR = rule -> {
        try (InputStream networkInputStream = new URL(rule.getUrl()).openStream()) {
            try (JarInputStream zipInputStream = new JarInputStream(networkInputStream, false)) {
                final Manifest manifest = zipInputStream.getManifest();
                if (manifest != null) {
                    //manifest.getMainAttributes().
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    };
}
