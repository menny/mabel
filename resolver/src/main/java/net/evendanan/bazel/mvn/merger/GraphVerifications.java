package net.evendanan.bazel.mvn.merger;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

public final class GraphVerifications {
  private GraphVerifications() {}

  public static void checkNoConflictingVersions(final Collection<Dependency> dependencies) {
    final Function<Dependency, String> dependencyKey =
        dependency ->
            String.format(
                Locale.US,
                "%s:%s",
                dependency.mavenCoordinate().groupId(),
                dependency.mavenCoordinate().artifactId());
    final Map<String, String> pinnedVersions = new HashMap<>();

    dependencies.forEach(
        dependency -> {
          final String pinnedVersion =
              pinnedVersions.putIfAbsent(
                  dependencyKey.apply(dependency), dependency.mavenCoordinate().version());
          if (pinnedVersion != null
              && !pinnedVersion.equals(dependency.mavenCoordinate().version())) {
            throw new InvalidGraphException(
                "NoConflictingVersions",
                dependency.mavenCoordinate(),
                "Pinned to "
                    + pinnedVersion
                    + " but needed "
                    + dependency.mavenCoordinate().version());
          }
        });
  }

  public static void checkAllGraphDependenciesAreResolved(Resolution resolution) {
    Set<MavenCoordinate> resolved = new HashSet<>();
    resolution.allResolvedDependencies().forEach(dep -> resolved.add(dep.mavenCoordinate()));

    GraphUtils.DfsTraveller(
        Collections.singleton(resolution),
        (dependency, level) -> {
          if (!resolved.contains(dependency.mavenCoordinate()))
            throw new GraphVerifications.InvalidGraphException(
                "AllGraphDependenciesAreResolved", dependency.mavenCoordinate());
        });
  }

  public static void checkGraphDoesNotHaveDanglingDependencies(Resolution resolution) {
    Set<MavenCoordinate> resolved = new HashSet<>();
    resolution.allResolvedDependencies().forEach(dep -> resolved.add(dep.mavenCoordinate()));

    GraphUtils.DfsTraveller(
        Collections.singleton(resolution),
        (dependency, level) -> resolved.remove(dependency.mavenCoordinate()));

    if (!resolved.isEmpty()) {
      throw new GraphVerifications.InvalidGraphException(
          "GraphDoesNotHaveDanglingDependencies", new ArrayList<>(resolved).get(0));
    }
  }

  public static void checkAllDependenciesAreResolved(Collection<Dependency> dependencies) {
    Set<MavenCoordinate> resolved = new HashSet<>();
    dependencies.forEach(dep -> resolved.add(dep.mavenCoordinate()));

    Optional<MavenCoordinate> first =
        dependencies.stream()
            .map(
                dependency ->
                    Triple.of(
                        dependency.dependencies().stream(),
                        dependency.exports().stream(),
                        dependency.runtimeDependencies().stream()))
            .flatMap(
                triple ->
                    Stream.concat(
                        Stream.concat(triple.getLeft(), triple.getMiddle()), triple.getRight()))
            .filter(mvn -> !resolved.contains(mvn))
            .findFirst();

    first.ifPresent(
        mvn -> {
          throw new GraphVerifications.InvalidGraphException("AllDependenciesAreResolved", mvn);
        });
  }

  public static void checkNoRepeatingDependencies(Collection<Dependency> dependencies) {
    Set<MavenCoordinate> seen = new HashSet<>();

    dependencies.forEach(
        resolved -> {
          if (!seen.add(resolved.mavenCoordinate())) {
            throw new GraphVerifications.InvalidGraphException(
                "NoRepeatingDependencies", resolved.mavenCoordinate());
          }
        });
  }

  public static void checkResolutionSuccess(Resolution resolution) {
    resolution.allResolvedDependencies().stream()
        .filter(d -> d.mavenCoordinate().equals(resolution.rootDependency()))
        .filter(d -> !StringUtils.isBlank(d.url()))
        .findFirst()
        .orElseThrow(
            () ->
                new InvalidGraphException(
                    "Failed to resolve requested coordinate", resolution.rootDependency()));
  }

  public static class InvalidGraphException extends RuntimeException {

    InvalidGraphException(
        String checkTitle, MavenCoordinate invalidDependency, String description) {
      super(constructErrorMessage(checkTitle, invalidDependency, description));
    }

    InvalidGraphException(String checkTitle, MavenCoordinate invalidDependency) {
      this(checkTitle, invalidDependency, null);
    }

    private static String constructErrorMessage(
        String checkTitle, MavenCoordinate invalidDependency, String description) {
      Preconditions.checkNotNull(checkTitle);
      String invalidMavenCoordinates =
          DependencyTools.DEFAULT.mavenCoordinates(Preconditions.checkNotNull(invalidDependency));

      if (Strings.isNullOrEmpty(description)) {
        return String.format(
            Locale.ROOT,
            "Verification '%s' failed for dependency '%s'.",
            checkTitle,
            invalidMavenCoordinates);
      } else {
        return String.format(
            Locale.ROOT,
            "Verification '%s' failed for dependency '%s'. Details: %s",
            checkTitle,
            invalidMavenCoordinates,
            description);
      }
    }
  }
}
