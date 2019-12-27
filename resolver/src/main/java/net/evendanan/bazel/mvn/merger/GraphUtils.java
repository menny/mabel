package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GraphUtils {

    static String printGraph(Collection<Dependency> dependencies) {
        final DependencyTools dependencyTools = new DependencyTools();
        final StringBuilder builder = new StringBuilder();

        DfsTraveller(dependencies,
                (dependency, level) -> {
                    for (int i = 0; i < level; i++) {
                        builder.append("  ");
                    }

                    builder.append(dependencyTools.mavenCoordinates(dependency)).append(System.lineSeparator());
                });

        return builder.toString();
    }

    public static void DfsTraveller(Collection<Dependency> dependencies, BiConsumer<Dependency, Integer> visitor) {
        DfsTraveller(dependencies, 1, visitor);
    }

    private static void DfsTraveller(Collection<Dependency> dependencies, int level, BiConsumer<Dependency, Integer> visitor) {
        dependencies.forEach(dependency -> {
            visitor.accept(dependency, level);
            final List<Dependency> children = new ArrayList<>();

            final Consumer<Dependency> childConsumer = child -> {
                if (!children.contains(child)) children.add(child);
            };
            dependency.getDependenciesList().forEach(childConsumer);
            dependency.getExportsList().forEach(childConsumer);
            dependency.getRuntimeDependenciesList().forEach(childConsumer);

            DfsTraveller(children, level + 1, visitor);
        });
    }


    static void BfsTraveller(Collection<Dependency> dependencies, BiConsumer<Dependency, Integer> visitor) {
        Queue<Dependency> queue = new ArrayDeque<>(dependencies);

        while (!queue.isEmpty()) {
            final Dependency dependency = queue.remove();
            visitor.accept(dependency, queue.size());

            final List<Dependency> children = new ArrayList<>();

            final Consumer<Dependency> childConsumer = child -> {
                if (!children.contains(child)) children.add(child);
            };
            dependency.getDependenciesList().forEach(childConsumer);
            dependency.getExportsList().forEach(childConsumer);
            dependency.getRuntimeDependenciesList().forEach(childConsumer);

            queue.addAll(children);
        }
    }

    static Collection<Dependency> deepCopyDeps(final Collection<Dependency> deps) {
        return deps.stream()
                .map(dependency -> Dependency.newBuilder(dependency)
                        .clearDependencies().addAllDependencies(deepCopyDeps(dependency.getDependenciesList()))
                        .clearExports().addAllExports(deepCopyDeps(dependency.getExportsList()))
                        .clearRuntimeDependencies().addAllRuntimeDependencies(deepCopyDeps(dependency.getRuntimeDependenciesList()))
                        .build())
                .collect(Collectors.toList());
    }

}
