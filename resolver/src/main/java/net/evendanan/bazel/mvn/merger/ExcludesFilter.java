package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;

import java.util.Collection;
import java.util.stream.Collectors;

public class ExcludesFilter {

    static Collection<Dependency> filterDependencies(
            final Collection<Dependency> dependencies, final Collection<String> excludes) {
        return dependencies.stream()
                .filter(
                        dependency ->
                                excludes.stream()
                                        .noneMatch(
                                                exclude ->
                                                        DependencyTools.DEFAULT
                                                                .mavenCoordinates(dependency)
                                                                .startsWith(exclude)))
                .map(
                        dependency ->
                                Dependency.builder(dependency)
                                        .dependencies(
                                                filterMavenDependencies(
                                                        dependency.dependencies(), excludes))
                                        .exports(
                                                filterMavenDependencies(
                                                        dependency.exports(), excludes))
                                        .runtimeDependencies(
                                                filterMavenDependencies(
                                                        dependency.runtimeDependencies(), excludes))
                                        .build())
                .collect(Collectors.toList());
    }

    private static Collection<MavenCoordinate> filterMavenDependencies(
            final Collection<MavenCoordinate> dependencies, final Collection<String> excludes) {
        return dependencies.stream()
                .filter(
                        dependency ->
                                excludes.stream()
                                        .noneMatch(
                                                exclude ->
                                                        DependencyTools.DEFAULT
                                                                .mavenCoordinates(dependency)
                                                                .startsWith(exclude)))
                .collect(Collectors.toList());
    }
}
