package net.evendanan.bazel.mvn.api.model;

import com.google.auto.value.AutoValue;

import java.util.Collection;

@AutoValue
public abstract class Resolution {
    public static Resolution create(
            TargetType type,
            MavenCoordinate rootDependency,
            Collection<Dependency> allResolvedDependencies) {
        return new AutoValue_Resolution(type, rootDependency, allResolvedDependencies);
    }

    public abstract TargetType targetType();

    public abstract MavenCoordinate rootDependency();

    public abstract Collection<Dependency> allResolvedDependencies();
}
