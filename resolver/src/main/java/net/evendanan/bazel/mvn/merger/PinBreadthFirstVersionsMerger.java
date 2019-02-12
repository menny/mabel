package net.evendanan.bazel.mvn.merger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.GraphMerger;

public class PinBreadthFirstVersionsMerger implements GraphMerger {
    private static String dependencyKey(Dependency dependency) {
        return String.format(Locale.US, "%s:%s", dependency.groupId(), dependency.artifactId());
    }

    private static Collection<Dependency> replaceWithPinned(Collection<Dependency> dependencies, Map<String, Dependency> cache, Map<String, Dependency> pinnedVersions) {
        return dependencies.stream()
                .map(dependency -> {
                    final String key = dependencyKey(dependency);
                    if (cache.containsKey(key)) {
                        return cache.get(key);
                    } else {
                        Dependency pinned = pinnedVersions.get(key);
                        final Dependency fixedDependency = new Dependency(pinned.groupId(), pinned.artifactId(), pinned.version(), pinned.packaging(),
                                replaceWithPinned(pinned.dependencies(), cache, pinnedVersions),
                                replaceWithPinned(pinned.exports(), cache, pinnedVersions),
                                replaceWithPinned(pinned.runtimeDependencies(), cache, pinnedVersions),
                                pinned.url(), pinned.sourcesUrl(), pinned.javadocUrl(), pinned.licenses());
                        cache.put(key, fixedDependency);
                        return fixedDependency;
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Dependency> mergeGraphs(final Collection<Dependency> dependencies) {
        Map<String, Dependency> pinnedVersions = new HashMap<>();

        GraphUtils.BfsTraveller(dependencies, (dependency, level) -> pinnedVersions.putIfAbsent(dependencyKey(dependency), dependency));

        return replaceWithPinned(dependencies, new HashMap<>(), pinnedVersions);
    }
}
