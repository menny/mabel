package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TargetCommenter {
    private final List<String> rootCoordinatesAsString;
    private final Map<String, List<String>> reverseDependencies;

    public TargetCommenter(Set<MavenCoordinate> rootCoordinates, Collection<Dependency> resolvedDependencies) {
        this.rootCoordinatesAsString = rootCoordinates
                .stream()
                .map(MavenCoordinate::toMavenString)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final Map<String, Set<String>> reverseDependenciesSet = new HashMap<>();
        resolvedDependencies.forEach(dependency -> {
            final String requesterMavenCoordinate = dependency.mavenCoordinate().toMavenString();
            collectReverseDependencies(requesterMavenCoordinate, dependency.dependencies(), reverseDependenciesSet);
            collectReverseDependencies(requesterMavenCoordinate, dependency.runtimeDependencies(), reverseDependenciesSet);
        });

        reverseDependencies = reverseDependenciesSet
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> new ArrayList<>(v.getValue())));
        reverseDependencies.entrySet().forEach(e -> e.getValue().sort(String::compareTo));
    }

    private static void collectReverseDependencies(String ownerKey, Collection<MavenCoordinate> deps, Map<String, Set<String>> reverseDependencies) {
        deps.forEach(dep ->
                reverseDependencies.compute(dep.toMavenString(),
                        (String depCoordinate, Set<String> requesters) -> {
                            if (requesters == null) {
                                requesters = new HashSet<>();
                            }
                            requesters.add(ownerKey);
                            return requesters;
                        }));
    }

    public TargetsBuilder createTargetBuilder(final TargetsBuilder baseBuilder) {
        return (dependency, dependencyTools) -> {
            final List<Target> targets = baseBuilder.buildTargets(dependency, dependencyTools);
            targets.forEach(this::addRootDependencyComment);
            targets.forEach(this::addRequiredByComment);
            return targets;
        };
    }

    private void addRequiredByComment(Target target) {
        final List<String> requesters = reverseDependencies.get(target.getMavenCoordinates());
        if (requesters != null) {
            requesters.forEach(requester -> target.addComment(String.format(Locale.ROOT, "This is a dependency of '%s'.", requester)));
        }
    }

    private void addRootDependencyComment(Target target) {
        if (rootCoordinatesAsString.contains(target.getMavenCoordinates())) {
            target.addComment("This is a root requested Maven artifact.");
        }
    }

}
