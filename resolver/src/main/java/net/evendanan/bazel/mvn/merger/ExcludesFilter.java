package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;

import java.util.Collection;
import java.util.stream.Collectors;

public class ExcludesFilter {

    static Collection<Dependency> filterDependencies(final Collection<Dependency> dependencies, final Collection<String> excludes) {
        return dependencies.stream()
                .filter(dependency -> excludes.stream().noneMatch(exclude -> DependencyTools.DEFAULT.mavenCoordinates(dependency).startsWith(exclude)))
                .map(dependency -> Dependency.newBuilder(dependency)
                        .clearDependencies().addAllDependencies(filterDependencies(dependency.getDependenciesList(), excludes))
                        .clearExports().addAllExports(filterDependencies(dependency.getExportsList(), excludes))
                        .clearRuntimeDependencies().addAllRuntimeDependencies(filterDependencies(dependency.getRuntimeDependenciesList(), excludes))
                        .build())
                .collect(Collectors.toList());
    }
}
