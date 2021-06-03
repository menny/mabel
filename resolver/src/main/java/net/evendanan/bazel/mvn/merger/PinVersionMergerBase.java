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

public abstract class PinVersionMergerBase implements GraphMerger {

    static String dependencyKey(MavenCoordinate mavenCoordinate) {
        return String.format(
                Locale.ROOT, "%s:%s", mavenCoordinate.groupId(), mavenCoordinate.artifactId());
    }

    @Override
    public Collection<Dependency> mergeGraphs(final Collection<Resolution> resolutions) {
        Map<String, Dependency> pinnedVersions = new HashMap<>();

        GraphUtils.DfsTraveller(
                resolutions,
                (dependency, level) ->
                        pinnedVersions.compute(
                                dependencyKey(dependency.mavenCoordinate()),
                                (key, previousDependency) ->
                                        pickDependency(previousDependency, dependency)));

        Function<Collection<MavenCoordinate>, Collection<MavenCoordinate>> convertDependencies =
                dependencies ->
                        dependencies.stream()
                                .map(
                                        mvn ->
                                                Preconditions.checkNotNull(
                                                                pinnedVersions.get(
                                                                        dependencyKey(mvn)))
                                                        .mavenCoordinate())
                                .distinct()
                                .collect(Collectors.toList());

        return resolutions.stream()
                .map(Resolution::allResolvedDependencies)
                .flatMap(Collection::stream)
                .map(original -> pinnedVersions.get(dependencyKey(original.mavenCoordinate())))
                .map(
                        original ->
                                Dependency.builder(original)
                                        .dependencies(
                                                convertDependencies.apply(original.dependencies()))
                                        .runtimeDependencies(
                                                convertDependencies.apply(
                                                        original.runtimeDependencies()))
                                        .build())
                .distinct()
                .collect(Collectors.toList());
    }

    protected abstract Dependency pickDependency(
            Dependency previousDependency, Dependency currentDependency);
}
