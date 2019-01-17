package net.evendanan.bazel.mvn.merger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.GraphMerger;

public class DuplicatesDepsRemovingMerger implements GraphMerger {

    private static Collection<Dependency> replaceWithPinned(Collection<Dependency> dependencies) {
        Map<String, Dependency> pinnedDeps = new HashMap<>();
        return dependencies.stream()
                .filter(dep -> pinnedDeps.putIfAbsent(dep.mavenCoordinates(), dep)==null)
                .map(dep -> new Dependency(dep.groupId(), dep.artifactId(), dep.version(), dep.packaging(),
                        replaceWithPinned(dep.dependencies()),
                        replaceWithPinned(dep.exports()),
                        replaceWithPinned(dep.runtimeDependencies()),
                        dep.url(), dep.sourcesUrl(), dep.javadocUrl(), dep.licenses()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Dependency> mergeGraphs(final Collection<Dependency> dependencies) {
        return replaceWithPinned(dependencies);
    }
}
