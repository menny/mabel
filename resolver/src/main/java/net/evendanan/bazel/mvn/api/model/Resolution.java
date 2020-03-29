package net.evendanan.bazel.mvn.api.model;

import com.google.auto.value.AutoValue;

import java.util.Collection;

@AutoValue
public abstract class Resolution {
    public static Resolution create(
            MavenCoordinate rootDependency, Collection<Dependency> allResolvedDependencies) {
        return new AutoValue_Resolution(rootDependency, allResolvedDependencies);
    }

    public abstract MavenCoordinate rootDependency();

    public abstract Collection<Dependency> allResolvedDependencies();
}
