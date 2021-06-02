package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TestOnlyMarker {
    public static Predicate<MavenCoordinate> mark(Collection<Resolution> resolutions, Set<MavenCoordinate> initialTestOnlyMap) {
        if (initialTestOnlyMap.isEmpty())
            return c -> initialTestOnlyMap.contains(strippedDownCoord(c));

        final Set<MavenCoordinate> marked = initialTestOnlyMap
                .stream()
                .map(TestOnlyMarker::strippedDownCoord)
                .collect(Collectors.toSet());

        resolutions.forEach(resolution -> {
            if (isTestOnlySubTree(marked, resolution.allResolvedDependencies(), resolution.rootDependency()) && !initialTestOnlyMap.contains(resolution.rootDependency())) {
                throw new GraphVerifications.InvalidGraphException("TestOnlyMarker", resolution.rootDependency(), "Dependency has test-only dependencies but is not a test-only artifact!");
            }
        });
        return c -> marked.contains(strippedDownCoord(c));
    }

    private static boolean isTestOnlySubTree(Set<MavenCoordinate> marked, Collection<Dependency> allResolvedDependencies, MavenCoordinate current) {
        final Dependency dependency = allResolvedDependencies
                .stream()
                .filter(d -> d.mavenCoordinate().equals(current))
                .findFirst()
                .get();
        /*NOTE: we have to go over the entire list, so the subtrees will be marked as wall*/
        final AtomicBoolean foundTestOnly = new AtomicBoolean(false);
        dependency.dependencies().forEach(child -> {
            if (isTestOnlySubTree(marked, allResolvedDependencies, child))
                foundTestOnly.set(true);
        });

        if (foundTestOnly.get()) {
            marked.add(strippedDownCoord(current));
            return true;
        } else {
            return marked.contains(strippedDownCoord(current));
        }
    }

    private static MavenCoordinate strippedDownCoord(MavenCoordinate coordinate) {
        return MavenCoordinate.create(coordinate.groupId(), coordinate.artifactId(), "", "");
    }
}
