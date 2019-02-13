package net.evendanan.bazel.mvn.merger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;

public class GraphUtils {

    static String printGraph(Collection<Dependency> dependencies) {
        final StringBuilder builder = new StringBuilder();

        DfsTraveller(dependencies,
                (dependency, level) -> {
                    for (int i = 0; i < level; i++) {
                        builder.append("  ");
                    }

                    builder.append(dependency.mavenCoordinates()).append(System.lineSeparator());
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
            dependency.dependencies().forEach(childConsumer);
            dependency.exports().forEach(childConsumer);
            dependency.runtimeDependencies().forEach(childConsumer);

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
            dependency.dependencies().forEach(childConsumer);
            dependency.exports().forEach(childConsumer);
            dependency.runtimeDependencies().forEach(childConsumer);

            queue.addAll(children);
        }
    }

    static Collection<Dependency> deepCopyDeps(final Collection<Dependency> deps) {
        return deps.stream()
                .map(dependency -> new Dependency(dependency.groupId(), dependency.artifactId(), dependency.version(), dependency.packaging(),
                        deepCopyDeps(dependency.dependencies()),
                        deepCopyDeps(dependency.exports()),
                        deepCopyDeps(dependency.runtimeDependencies()),
                        dependency.url(), dependency.sourcesUrl(), dependency.javadocUrl(), dependency.licenses()))
                .collect(Collectors.toList());
    }

}
