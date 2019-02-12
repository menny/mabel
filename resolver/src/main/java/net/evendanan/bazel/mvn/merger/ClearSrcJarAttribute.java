package net.evendanan.bazel.mvn.merger;

import java.net.URI;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.evendanan.bazel.mvn.api.Dependency;

public class ClearSrcJarAttribute {
    public static Collection<Dependency> clearSrcJar(final Collection<Dependency> dependencies) {
        return clearSrcJar(dependencies, new MemoizeDependency());
    }

    private static Collection<Dependency> clearSrcJar(final Collection<Dependency> dependencies, MemoizeDependency memoizeDependency) {
        return dependencies.stream()
                .map(memoizeDependency::map)
                .collect(Collectors.toList());
    }

    private static class MemoizeDependency extends GraphMemoizator {

        @Nonnull
        @Override
        protected Dependency calculate(@Nonnull final Dependency original) {
            return new Dependency(original.groupId(), original.artifactId(), original.version(), original.packaging(),
                    clearSrcJar(original.dependencies(), this),
                    clearSrcJar(original.exports(), this),
                    clearSrcJar(original.runtimeDependencies(), this),
                    original.url(), URI.create(""), original.javadocUrl(), original.licenses());
        }

        @Override
        protected String getKeyForDependency(final Dependency dependency) {
            return dependency.mavenCoordinates();
        }
    }
}
