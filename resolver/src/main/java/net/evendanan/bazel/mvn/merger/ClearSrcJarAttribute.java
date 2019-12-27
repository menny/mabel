package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.stream.Collectors;

public class ClearSrcJarAttribute {
    public static Collection<Dependency> clearSrcJar(final Collection<Dependency> dependencies) {
        return clearSrcJar(dependencies, new MemoizeDependency());
    }

    private static Collection<Dependency> clearSrcJar(final Collection<Dependency> dependencies, MemoizeDependency memoizeDependency) {
        return dependencies.stream()
                .map(memoizeDependency::map)
                .collect(Collectors.toList());
    }

    private static class MemoizeDependency extends GraphMemoizator<Dependency> {

        @Nonnull
        @Override
        protected Dependency calculate(@Nonnull final Dependency original) {
            return Dependency.newBuilder(original)
                    .clearDependencies().addAllDependencies(clearSrcJar(original.getDependenciesList(), this))
                    .clearExports().addAllExports(clearSrcJar(original.getExportsList(), this))
                    .clearRuntimeDependencies().addAllRuntimeDependencies(clearSrcJar(original.getRuntimeDependenciesList(), this))
                    .clearSourcesUrl()
                    .build();
        }

        @Override
        protected String getKeyForObject(final Dependency dependency) {
            return DependencyTools.DEFAULT.mavenCoordinates(dependency);
        }
    }
}
