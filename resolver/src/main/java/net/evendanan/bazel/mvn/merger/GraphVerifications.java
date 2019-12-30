package net.evendanan.bazel.mvn.merger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.Resolution;

public class GraphVerifications {

    static void checkNoConflictingVersions(final Collection<Dependency> dependencies) {
        final Function<Dependency, String> dependencyKey = dependency -> String.format(Locale.US, "%s:%s", dependency.getMavenCoordinate().getGroupId(), dependency.getMavenCoordinate().getArtifactId());
        final Map<String, String> pinnedVersions = new HashMap<>();

        dependencies.forEach(dependency -> {
            final String pinnedVersion = pinnedVersions.putIfAbsent(dependencyKey.apply(dependency), dependency.getMavenCoordinate().getVersion());
            if (pinnedVersion != null && !pinnedVersion.equals(dependency.getMavenCoordinate().getVersion())) {
                throw new IllegalStateException("Dependency " + DependencyTools.DEFAULT.mavenCoordinates(dependency) + " is pinned to " + pinnedVersion + " but needed " + dependency.getMavenCoordinate().getVersion());
            }
        });
    }

    static void checkAllGraphDependenciesAreResolved(Resolution resolution) {

    }

    static void checkGraphDoesHaveDanglingDependencies(Resolution resolution) {

    }

    static void checkAllDependenciesAreResolved(Collection<Dependency> dependencies) {

    }

    static void checkNoRepeatingDependencies(Collection<Dependency> dependencies) {

    }
}
