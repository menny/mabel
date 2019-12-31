package net.evendanan.bazel.mvn.merger;

import com.google.common.base.Preconditions;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.GraphMerger;
import net.evendanan.bazel.mvn.api.MavenCoordinate;
import net.evendanan.bazel.mvn.api.Resolution;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PinBreadthFirstVersionsMerger implements GraphMerger {
    private static String dependencyKey(MavenCoordinate mavenCoordinate) {
        return String.format(Locale.ROOT, "%s:%s", mavenCoordinate.getGroupId(), mavenCoordinate.getArtifactId());
    }

    @Override
    public Collection<Dependency> mergeGraphs(final Collection<Resolution> resolutions) {
        Map<String, Dependency> pinnedVersions = new HashMap<>();

        GraphUtils.BfsTraveller(resolutions, (dependency, level) -> pinnedVersions.compute(dependencyKey(dependency.getMavenCoordinate()),
                (key, previousDep) -> {
                    if (previousDep == null || previousDep.getUrl().equals("")) return dependency;
                    else return previousDep;
                }));

        Function<List<MavenCoordinate>, List<MavenCoordinate>> convertDependencies = dependencies -> dependencies.stream()
                .map(mvn -> Preconditions.checkNotNull(pinnedVersions.get(dependencyKey(mvn))).getMavenCoordinate())
                .distinct()
                .collect(Collectors.toList());

        return resolutions.stream()
                .map(Resolution::getAllResolvedDependenciesList)
                .flatMap(List::stream)
                .map(original -> pinnedVersions.get(dependencyKey(original.getMavenCoordinate())))
                .map(original -> Dependency.newBuilder(original)
                        .clearDependencies().addAllDependencies(convertDependencies.apply(original.getDependenciesList()))
                        .clearExports().addAllExports(convertDependencies.apply(original.getExportsList()))
                        .clearRuntimeDependencies().addAllRuntimeDependencies(convertDependencies.apply(original.getRuntimeDependenciesList()))
                        .build())
                .distinct()
                .collect(Collectors.toList());
    }
}
