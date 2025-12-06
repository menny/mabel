package net.evendanan.bazel.mvn.api;

import java.util.Locale;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;

public class DependencyTools {

  public static final DependencyTools DEFAULT = new DependencyTools();

  private static String normalize(String name) {
    return name.replaceAll("[+.-]", "_");
  }

  public final String mavenCoordinates(Dependency dependency) {
    return mavenCoordinates(dependency.mavenCoordinate());
  }

  public String mavenCoordinates(MavenCoordinate mavenCoordinate) {
    return String.format(
        Locale.ROOT,
        "%s:%s:%s",
        mavenCoordinate.groupId(),
        mavenCoordinate.artifactId(),
        mavenCoordinate.version());
  }

  public final String repositoryRuleName(Dependency dependency) {
    return repositoryRuleName(dependency.mavenCoordinate());
  }

  public String repositoryRuleName(MavenCoordinate mavenCoordinate) {
    return String.format(
        Locale.ROOT,
        "%s__%s__%s",
        normalize(mavenCoordinate.groupId()),
        normalize(mavenCoordinate.artifactId()),
        normalize(mavenCoordinate.version()));
  }

  public final String targetName(Dependency dependency) {
    return targetName(dependency.mavenCoordinate());
  }

  public String targetName(MavenCoordinate mavenCoordinate) {
    return String.format(
        Locale.ROOT,
        "%s__%s",
        normalize(mavenCoordinate.groupId()),
        normalize(mavenCoordinate.artifactId()));
  }
}
