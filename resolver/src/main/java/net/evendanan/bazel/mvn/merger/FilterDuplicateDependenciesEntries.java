package net.evendanan.bazel.mvn.merger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.evendanan.bazel.mvn.api.Dependency;

public class FilterDuplicateDependenciesEntries {

    private static Collection<Dependency> filterDuplicateEntries(Collection<Dependency> dependencies, MemoizeDependency memoization) {
        Set<String> pinnedDeps = new HashSet<>();
        return dependencies.stream()
                .filter(dep -> pinnedDeps.add(dep.mavenCoordinates()))
                .map(memoization::map)
                .collect(Collectors.toList());
    }

    public Collection<Dependency> filterDuplicateDependencies(final Collection<Dependency> dependencies) {
        return filterDuplicateEntries(dependencies, new MemoizeDependency());
    }

    private static class MemoizeDependency extends GraphMemoizator {

        @Nonnull
        @Override
        protected Dependency calculate(@Nonnull final Dependency original) {
            return new Dependency(original.groupId(), original.artifactId(), original.version(), original.packaging(),
                    filterDuplicateEntries(original.dependencies(), this),
                    filterDuplicateEntries(original.exports(), this),
                    filterDuplicateEntries(original.runtimeDependencies(), this),
                    original.url(), original.sourcesUrl(), original.javadocUrl(), original.licenses());
        }

        @Override
        protected String getKeyForDependency(final Dependency dependency) {
            return dependency.mavenCoordinates();
        }
    }
}
