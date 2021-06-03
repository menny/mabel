package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.model.Dependency;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterDuplicateDependenciesEntries {

    private static Set<Dependency> filterDuplicateEntries(
            Collection<Dependency> dependencies, MemoizeDependency memoization) {
        Set<String> pinnedDeps = new HashSet<>();
        return dependencies.stream()
                .filter(dep -> pinnedDeps.add(DependencyTools.DEFAULT.mavenCoordinates(dep)))
                .map(memoization::map)
                .collect(Collectors.toSet());
    }

    public static Set<Dependency> filterDuplicateDependencies(
            final Collection<Dependency> dependencies) {
        return filterDuplicateEntries(dependencies, new MemoizeDependency());
    }

    private static class MemoizeDependency extends GraphMemoizator<Dependency> {
        @Nonnull
        @Override
        protected Dependency calculate(@Nonnull final Dependency original) {
            return Dependency.builder(original)
                    .dependencies(
                            original.dependencies().stream()
                                    .distinct()
                                    .collect(Collectors.toList()))
                    .runtimeDependencies(
                            original.runtimeDependencies().stream()
                                    .distinct()
                                    .collect(Collectors.toList()))
                    .build();
        }

        @Override
        protected String getKeyForObject(final Dependency dependency) {
            return DependencyTools.DEFAULT.mavenCoordinates(dependency);
        }
    }
}
