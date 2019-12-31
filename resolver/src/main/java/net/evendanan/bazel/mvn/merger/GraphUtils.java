package net.evendanan.bazel.mvn.merger;

import com.google.common.base.Preconditions;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.MavenCoordinate;
import net.evendanan.bazel.mvn.api.Resolution;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class GraphUtils {

    static String printGraph(Resolution resolution) {
        return printGraph(Collections.singleton(resolution));
    }

    static String printGraph(Collection<Resolution> resolutions) {
        final DependencyTools dependencyTools = new DependencyTools();
        final StringBuilder builder = new StringBuilder();

        DfsTraveller(resolutions,
                (dependency, level) -> {
                    for (int i = 0; i < level; i++) {
                        builder.append("  ");
                    }

                    builder.append(dependencyTools.mavenCoordinates(dependency)).append(System.lineSeparator());
                });

        return builder.toString();
    }

    public static void DfsTraveller(Collection<Resolution> resolutions, BiConsumer<Dependency, Integer> visitor) {
        Map<MavenCoordinate, Dependency> mapper = new HashMap<>();
        resolutions.forEach(resolution -> resolution.getAllResolvedDependenciesList().forEach(dep -> mapper.put(dep.getMavenCoordinate(), dep)));

        resolutions.forEach(resolution -> DfsTraveller(resolution.getRootDependency(), mapper::get, 1, visitor));
    }

    private static void DfsTraveller(MavenCoordinate mavenCoordinate, Function<MavenCoordinate, Dependency> dependencyMap, int level, BiConsumer<Dependency, Integer> visitor) {
        Dependency dependency = Preconditions.checkNotNull(dependencyMap.apply(mavenCoordinate), "Can not find mapping for " + mavenCoordinate);
        visitor.accept(dependency, level);

        Stream.concat(
                Stream.concat(
                        dependency.getDependenciesList().stream(),
                        dependency.getExportsList().stream()),
                dependency.getRuntimeDependenciesList().stream())
                .distinct()
                .forEach(child -> DfsTraveller(child, dependencyMap, level + 1, visitor));
    }

    static void BfsTraveller(Collection<Resolution> resolutions, BiConsumer<Dependency, Integer> visitor) {
        Map<MavenCoordinate, Dependency> mapper = new HashMap<>();
        resolutions.forEach(resolution -> resolution.getAllResolvedDependenciesList().forEach(dep -> mapper.put(dep.getMavenCoordinate(), dep)));

        Queue<MavenCoordinate> queue = new ArrayDeque<>();
        resolutions.forEach(resolution -> queue.add(resolution.getRootDependency()));

        while (!queue.isEmpty()) {
            final Dependency dependency = Preconditions.checkNotNull(mapper.get(queue.remove()), "Can not find mapping for " + queue.peek());
            visitor.accept(dependency, queue.size());

            Stream.concat(
                    Stream.concat(
                            dependency.getDependenciesList().stream(),
                            dependency.getExportsList().stream()),
                    dependency.getRuntimeDependenciesList().stream())
                    .distinct()
                    .forEach(queue::add);
        }
    }

}
