package net.evendanan.bazel.mvn.merger;

import java.net.URI;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.evendanan.bazel.mvn.api.Dependency;

public class DependencyNamePrefixer {

    public static Collection<Dependency> wrap(final Collection<Dependency> dependencies, final String prefix) {
        return wrap(dependencies, new MemoizeDependency(prefix));
    }

    private static Collection<Dependency> wrap(final Collection<Dependency> dependencies, MemoizeDependency memoizeDependency) {
        return dependencies.stream()
                .map(memoizeDependency::map)
                .collect(Collectors.toList());
    }

    private static class MemoizeDependency extends GraphMemoizator {

        private final String prefix;

        MemoizeDependency(final String prefix) {
            this.prefix = prefix;
        }

        @Nonnull
        @Override
        protected Dependency calculate(@Nonnull final Dependency original) {
            return new DependencyWithPrefix(original.groupId(), original.artifactId(), original.version(), original.packaging(),
                    wrap(original.dependencies(), this),
                    wrap(original.exports(), this),
                    wrap(original.runtimeDependencies(), this),
                    original.url(), original.sourcesUrl(), original.javadocUrl(), original.licenses(), prefix);
        }

        @Override
        protected String getKeyForDependency(final Dependency dependency) {
            return dependency.mavenCoordinates();
        }
    }

    private static class DependencyWithPrefix extends Dependency {

        private final String prefix;

        private DependencyWithPrefix(final String groupId, final String artifactId, final String version, final String packaging,
                                     final Collection<Dependency> dependencies, final Collection<Dependency> exports, final Collection<Dependency> runtimeDependencies,
                                     final URI url, final URI sourcesUrl, final URI javadocUrl,
                                     final Collection<License> licenses,
                                     String prefix) {
            super(groupId, artifactId, version, packaging, dependencies, exports, runtimeDependencies, url, sourcesUrl, javadocUrl, licenses);
            this.prefix = prefix;
        }

        @Override
        public String repositoryRuleName() {
            return prefix + super.repositoryRuleName();
        }

        @Override
        public String targetName() {
            return prefix + super.targetName();
        }
    }
}
