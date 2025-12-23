package net.evendanan.bazel.mvn.api;

import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;

public class DependencyTools {

  public static final DependencyTools DEFAULT = new DependencyTools();

  private static String normalize(String name) {
    StringBuilder sb = new StringBuilder(name.length());
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (c == '+' || c == '.' || c == '-') {
        sb.append('_');
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public final String mavenCoordinates(Dependency dependency) {
    return mavenCoordinates(dependency.mavenCoordinate());
  }

  public String mavenCoordinates(MavenCoordinate mavenCoordinate) {
    return mavenCoordinate.groupId()
        + ":"
        + mavenCoordinate.artifactId()
        + ":"
        + mavenCoordinate.version();
  }

  public final String repositoryRuleName(Dependency dependency) {
    return repositoryRuleName(dependency.mavenCoordinate());
  }

  public String repositoryRuleName(MavenCoordinate mavenCoordinate) {
    return normalize(mavenCoordinate.groupId())
        + "__"
        + normalize(mavenCoordinate.artifactId())
        + "__"
        + normalize(mavenCoordinate.version());
  }

  public final String targetName(Dependency dependency) {
    return targetName(dependency.mavenCoordinate());
  }

  public String targetName(MavenCoordinate mavenCoordinate) {
    return normalize(mavenCoordinate.groupId()) + "__" + normalize(mavenCoordinate.artifactId());
  }
}
