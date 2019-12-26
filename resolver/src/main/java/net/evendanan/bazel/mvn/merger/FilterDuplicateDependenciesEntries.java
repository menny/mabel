package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterDuplicateDependenciesEntries {

    private static Collection<Dependency> filterDuplicateEntries(Collection<Dependency> dependencies, MemoizeDependency memoization) {
        Set<String> pinnedDeps = new HashSet<>();
        return dependencies.stream()
                .filter(dep -> pinnedDeps.add(DependencyTools.DEFAULT.mavenCoordinates(dep)))
                .map(memoization::map)
                .collect(Collectors.toList());
    }

    public static Collection<Dependency> filterDuplicateDependencies(final Collection<Dependency> dependencies) {
        return filterDuplicateEntries(dependencies, new MemoizeDependency());
    }

    private static class MemoizeDependency extends GraphMemoizator<Dependency> {
        @Nonnull
        @Override
        protected Dependency calculate(@Nonnull final Dependency original) {
            return Dependency.newBuilder(original)
                    .clearDependencies().addAllDependencies(filterDuplicateEntries(original.getDependenciesList(), this))
                    .clearExports().addAllExports(filterDuplicateEntries(original.getExportsList(), this))
                    .clearRuntimeDependencies().addAllRuntimeDependencies(filterDuplicateEntries(original.getRuntimeDependenciesList(), this))
                    .build();
        }

        @Override
        protected String getKeyForObject(final Dependency dependency) {
            return DependencyTools.DEFAULT.mavenCoordinates(dependency);
        }
    }
}
