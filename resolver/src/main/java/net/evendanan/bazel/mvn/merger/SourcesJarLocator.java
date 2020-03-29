package net.evendanan.bazel.mvn.merger;

import com.google.common.annotations.VisibleForTesting;
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.model.Dependency;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SourcesJarLocator {

    private static final String SOURCES_CLASSIFIER = "sources";

    private final ConnectionFactory mConnectionFactory;
    private final Map<String, String> mURLCache = new HashMap<>();

    public SourcesJarLocator() {
        this(url -> (HttpURLConnection) url.openConnection());
    }

    @VisibleForTesting
    SourcesJarLocator(final ConnectionFactory opener) {
        mConnectionFactory = opener;
    }

    private static Collection<Dependency> fillSourcesAttribute(
            Collection<Dependency> dependencies, DependencyMemoizator memoizator) {
        return dependencies.stream().map(memoizator::map).collect(Collectors.toList());
    }

    public Collection<Dependency> fillSourcesAttribute(Collection<Dependency> dependencies) {
        return fillSourcesAttribute(dependencies, new DependencyMemoizator());
    }

    private String uriWithClassifier(final String uri) {
        return mURLCache.computeIfAbsent(uri, this::hotFetch);
    }

    private String hotFetch(final String url) {
        System.out.print('.');
        final int extStartIndex = url.lastIndexOf(".");
        if (extStartIndex > 0) {
            try {
                // for example
                // https://repo1.maven.org/maven2/com/google/guava/guava/20.0/guava-20.0.jar
                // will become
                // https://repo1.maven.org/maven2/com/google/guava/guava/20.0/guava-20.0-sources.jar
                // (always jar ext)
                final String urlWithClassifier =
                        String.format(
                                Locale.US,
                                "%s-%s.jar",
                                url.substring(0, extStartIndex),
                                SOURCES_CLASSIFIER);
                final URL classifiedUrl = new URL(urlWithClassifier);
                HttpURLConnection con = mConnectionFactory.openUrlConnection(classifiedUrl);
                con.setRequestMethod("HEAD");
                final int responseCode = con.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    return classifiedUrl.toString();
                } else {
                    return "";
                }
            } catch (Exception e) {
                return "";
            }
        } else {
            return "";
        }
    }

    interface ConnectionFactory {
        HttpURLConnection openUrlConnection(URL url) throws IOException;
    }

    private class DependencyMemoizator extends GraphMemoizator<Dependency> {

        private final DependencyTools mDependencyTools = new DependencyTools();

        @Nonnull
        @Override
        protected Dependency calculate(@Nonnull final Dependency original) {
            return Dependency.builder(original)
                    .sourcesUrl(uriWithClassifier(original.url()))
                    .build();
        }

        @Override
        protected String getKeyForObject(final Dependency dependency) {
            return mDependencyTools.mavenCoordinates(dependency);
        }
    }
}
