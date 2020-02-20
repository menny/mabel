package net.evendanan.bazel.mvn.merger;

import com.google.common.base.Preconditions;
import net.evendanan.bazel.mvn.api.GraphMerger;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PinBreadthFirstVersionsMerger implements GraphMerger {
    private static String dependencyKey(MavenCoordinate mavenCoordinate) {
        return String.format(Locale.ROOT, "%s:%s", mavenCoordinate.groupId(), mavenCoordinate.artifactId());
    }

    @Override
    public Collection<Dependency> mergeGraphs(final Collection<Resolution> resolutions) {
        Map<String, Dependency> pinnedVersions = new HashMap<>();

        GraphUtils.BfsTraveller(resolutions, (dependency, level) -> pinnedVersions.compute(dependencyKey(dependency.mavenCoordinate()),
                (key, previousDep) -> {
                    if (previousDep == null || previousDep.url().isEmpty()) return dependency;
                    else return previousDep;
                }));

        Function<Collection<MavenCoordinate>, Collection<MavenCoordinate>> convertDependencies = dependencies -> dependencies.stream()
                .map(mvn -> Preconditions.checkNotNull(pinnedVersions.get(dependencyKey(mvn))).mavenCoordinate())
                .distinct()
                .collect(Collectors.toList());

        return resolutions.stream()
                .map(Resolution::allResolvedDependencies)
                .flatMap(Collection::stream)
                .map(original -> pinnedVersions.get(dependencyKey(original.mavenCoordinate())))
                .map(original -> Dependency.builder(original)
                        .dependencies(convertDependencies.apply(original.dependencies()))
                        .exports(convertDependencies.apply(original.exports()))
                        .runtimeDependencies(convertDependencies.apply(original.runtimeDependencies()))
                        .build())
                .distinct()
                .collect(Collectors.toList());
    }
}
