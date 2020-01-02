package net.evendanan.bazel.mvn.merger;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.MavenCoordinate;
import net.evendanan.bazel.mvn.api.Resolution;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public final class GraphVerifications {
    private GraphVerifications() {
    }

    public static class InvalidGraphException extends RuntimeException {

        private static String constructErrorMessage(String checkTitle, MavenCoordinate invalidDependency, String description) {
            Preconditions.checkNotNull(checkTitle);
            String invalidMavenCoordinates = DependencyTools.DEFAULT.mavenCoordinates(Preconditions.checkNotNull(invalidDependency));

            if (Strings.isNullOrEmpty(description)) {
                return String.format(Locale.ROOT, "Verification '%s' failed for dependency '%s'.", checkTitle, invalidMavenCoordinates);
            } else {
                return String.format(Locale.ROOT, "Verification '%s' failed for dependency '%s'. Details: %s", checkTitle, invalidMavenCoordinates, description);
            }
        }

        InvalidGraphException(String checkTitle, MavenCoordinate invalidDependency, String description) {
            super(constructErrorMessage(checkTitle, invalidDependency, description));
        }

        InvalidGraphException(String checkTitle, MavenCoordinate invalidDependency) {
            this(checkTitle, invalidDependency, null);
        }
    }

    public static void checkNoConflictingVersions(final Collection<Dependency> dependencies) {
        final Function<Dependency, String> dependencyKey = dependency -> String.format(Locale.US, "%s:%s", dependency.getMavenCoordinate().getGroupId(), dependency.getMavenCoordinate().getArtifactId());
        final Map<String, String> pinnedVersions = new HashMap<>();

        dependencies.forEach(dependency -> {
            final String pinnedVersion = pinnedVersions.putIfAbsent(dependencyKey.apply(dependency), dependency.getMavenCoordinate().getVersion());
            if (pinnedVersion != null && !pinnedVersion.equals(dependency.getMavenCoordinate().getVersion())) {
                throw new InvalidGraphException(
                        "NoConflictingVersions",
                        dependency.getMavenCoordinate(),
                        "Pinned to " + pinnedVersion + " but needed " + dependency.getMavenCoordinate().getVersion());
            }
        });
    }

    public static void checkAllGraphDependenciesAreResolved(Resolution resolution) {
        Set<MavenCoordinate> resolved = new HashSet<>();
        resolution.getAllResolvedDependenciesList().forEach(dep -> resolved.add(dep.getMavenCoordinate()));

        GraphUtils.DfsTraveller(Collections.singleton(resolution), (dependency, level) -> {
            if (!resolved.contains(dependency.getMavenCoordinate()))
                throw new GraphVerifications.InvalidGraphException("AllGraphDependenciesAreResolved", dependency.getMavenCoordinate());
        });
    }

    public static void checkGraphDoesNotHaveDanglingDependencies(Resolution resolution) {
        Set<MavenCoordinate> resolved = new HashSet<>();
        resolution.getAllResolvedDependenciesList().forEach(dep -> resolved.add(dep.getMavenCoordinate()));

        GraphUtils.DfsTraveller(Collections.singleton(resolution), (dependency, level) -> resolved.remove(dependency.getMavenCoordinate()));

        if (!resolved.isEmpty()) {
            throw new GraphVerifications.InvalidGraphException("GraphDoesNotHaveDanglingDependencies", new ArrayList<>(resolved).get(0));
        }
    }

    public static void checkAllDependenciesAreResolved(Collection<Dependency> dependencies) {
        Set<MavenCoordinate> resolved = new HashSet<>();
        dependencies.forEach(dep -> resolved.add(dep.getMavenCoordinate()));

        Optional<MavenCoordinate> first = dependencies.stream()
                .map(dependency -> Triple.of(
                        dependency.getDependenciesList().stream(),
                        dependency.getExportsList().stream(),
                        dependency.getRuntimeDependenciesList().stream()))
                .flatMap(triple -> Stream.concat(Stream.concat(triple.getLeft(), triple.getMiddle()), triple.getRight()))
                .filter(mvn -> !resolved.contains(mvn))
                .findFirst();

        first.ifPresent(mvn -> {
            throw new GraphVerifications.InvalidGraphException("AllDependenciesAreResolved", mvn);
        });
    }

    public static void checkNoRepeatingDependencies(Collection<Dependency> dependencies) {
        Set<MavenCoordinate> seen = new HashSet<>();

        dependencies.forEach(resolved -> {
            if (!seen.add(resolved.getMavenCoordinate())) {
                throw new GraphVerifications.InvalidGraphException("NoRepeatingDependencies", resolved.getMavenCoordinate());
            }
        });
    }
}
