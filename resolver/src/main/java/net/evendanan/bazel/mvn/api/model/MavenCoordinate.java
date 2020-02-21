package net.evendanan.bazel.mvn.api.model;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MavenCoordinate {
    public abstract String groupId();

    public abstract String artifactId();

    public abstract String version();

    public abstract String packaging();

    public static MavenCoordinate create(String groupId, String artifactId, String version, String packaging) {
        return new AutoValue_MavenCoordinate(groupId, artifactId, version, packaging);
    }
}
