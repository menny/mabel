package net.evendanan.bazel.mvn.merger;

import java.util.Collection;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;

public class ExcludesFilter {

    static Collection<Dependency> filterDependencies(final Collection<Dependency> dependencies, final Collection<String> excludes) {
        return dependencies.stream()
                .filter(dependency -> excludes.stream().noneMatch(exclude -> dependency.mavenCoordinates().startsWith(exclude)))
                .map(dependency -> new Dependency(dependency.groupId(), dependency.artifactId(), dependency.version(), dependency.packaging(),
                        filterDependencies(dependency.dependencies(), excludes),
                        filterDependencies(dependency.exports(), excludes),
                        filterDependencies(dependency.runtimeDependencies(), excludes),
                        dependency.url(), dependency.sourcesUrl(), dependency.javadocUrl(), dependency.licenses()))
                .collect(Collectors.toList());
    }
}
