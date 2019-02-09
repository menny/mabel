package net.evendanan.bazel.mvn.merger;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;

public class SourcesJarLocator {

    private static final String SOURCES_CLASSIFIER = "sources";

    private final ConnectionOpener mConnectionOpenner;
    private final Map<URI, URI> mURLCache = new HashMap<>();

    public SourcesJarLocator() {
        this(url -> (HttpURLConnection) url.openConnection());
    }

    @VisibleForTesting
    SourcesJarLocator(final ConnectionOpener opener) {
        mConnectionOpenner = opener;
    }

    public Collection<Dependency> fillSourcesAttribute(Collection<Dependency> dependencies) {
        return dependencies.stream()
                .map(dependency -> new Dependency(dependency.groupId(), dependency.artifactId(), dependency.version(), dependency.packaging(),
                        fillSourcesAttribute(dependency.dependencies()),
                        fillSourcesAttribute(dependency.exports()),
                        fillSourcesAttribute(dependency.runtimeDependencies()),
                        dependency.url(), uriWithClassifier(dependency.url()), dependency.javadocUrl(), dependency.licenses()))
                .collect(Collectors.toList());
    }

    private URI uriWithClassifier(final URI uri) {
        return mURLCache.computeIfAbsent(uri, this::hotFetch);
    }

    private URI hotFetch(final URI uri) {
        System.out.print('.');
        final String url = uri.toASCIIString();
        final int extStartIndex = url.lastIndexOf(".");
        if (extStartIndex > 0) {
            try {
                //for example
                // https://repo1.maven.org/maven2/com/google/guava/guava/20.0/guava-20.0.jar
                // will become
                // https://repo1.maven.org/maven2/com/google/guava/guava/20.0/guava-20.0-sources.jar (always jar ext)
                final String urlWithClassifier = String.format(Locale.US, "%s-%s.jar", url.substring(0, extStartIndex), SOURCES_CLASSIFIER);
                final URL classifiedUrl = new URL(urlWithClassifier);
                HttpURLConnection con = mConnectionOpenner.openUrlConnection(classifiedUrl);
                con.setRequestMethod("HEAD");
                final int responseCode = con.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    return classifiedUrl.toURI();
                } else {
                    return URI.create("");
                }
            } catch (Exception e) {
                return URI.create("");
            }
        } else {
            return URI.create("");
        }
    }

    interface ConnectionOpener {
        HttpURLConnection openUrlConnection(URL url) throws IOException;
    }
}
