package net.evendanan.bazel.mvn.merger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.GraphMerger;

public class FilterDuplicateDependenciesEntries implements GraphMerger {

    private static Collection<Dependency> filterDuplicateEntries(Collection<Dependency> dependencies, Map<String, Dependency> cache) {
        Set<String> pinnedDeps = new HashSet<>();
        return dependencies.stream()
                .filter(dep -> pinnedDeps.add(dep.mavenCoordinates()))
                .map(dep -> {
                    if (cache.containsKey(dep.mavenCoordinates())) {
                        return cache.get(dep.mavenCoordinates());
                    } else {
                        final Dependency dependency = new Dependency(dep.groupId(), dep.artifactId(), dep.version(), dep.packaging(),
                                filterDuplicateEntries(dep.dependencies(), cache),
                                filterDuplicateEntries(dep.exports(), cache),
                                filterDuplicateEntries(dep.runtimeDependencies(), cache),
                                dep.url(), dep.sourcesUrl(), dep.javadocUrl(), dep.licenses());
                        cache.put(dep.mavenCoordinates(), dependency);
                        return dependency;
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Dependency> mergeGraphs(final Collection<Dependency> dependencies) {
        return filterDuplicateEntries(dependencies, new HashMap<>());
    }
}
