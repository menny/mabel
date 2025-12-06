package net.evendanan.bazel.mvn.api.model;

import com.google.auto.value.AutoValue;
import java.util.Locale;

@AutoValue
public abstract class MavenCoordinate {
  public static MavenCoordinate create(
      String groupId, String artifactId, String version, String packaging) {
    return new AutoValue_MavenCoordinate(groupId, artifactId, version, packaging);
  }

  public abstract String groupId();

  public abstract String artifactId();

  public abstract String version();

  public abstract String packaging();

  public String toMavenString() {
    return String.format(Locale.ROOT, "%s:%s:%s", groupId(), artifactId(), version());
  }
}
