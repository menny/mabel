package net.evendanan.bazel.mvn.api.model;

import com.google.auto.value.AutoValue;

import java.util.Collection;
import java.util.Collections;

@AutoValue
public abstract class Dependency {
    public static Builder builder() {
        return new AutoValue_Dependency.Builder()
                .dependencies(Collections.emptyList())
                .exports(Collections.emptyList())
                .runtimeDependencies(Collections.emptyList())
                .url("")
                .sourcesUrl("")
                .javadocUrl("")
                .licenses(Collections.emptyList());
    }

    public static Builder builder(Dependency original) {
        return new AutoValue_Dependency.Builder()
                .mavenCoordinate(original.mavenCoordinate())
                .dependencies(original.dependencies())
                .exports(original.exports())
                .runtimeDependencies(original.runtimeDependencies())
                .url(original.url())
                .sourcesUrl(original.sourcesUrl())
                .javadocUrl(original.javadocUrl())
                .licenses(original.licenses());
    }

    public abstract MavenCoordinate mavenCoordinate();

    public abstract Collection<MavenCoordinate> dependencies();

    public abstract Collection<MavenCoordinate> exports();

    public abstract Collection<MavenCoordinate> runtimeDependencies();

    public abstract String url();

    public abstract String sourcesUrl();

    public abstract String javadocUrl();

    public abstract Collection<License> licenses();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder mavenCoordinate(MavenCoordinate mavenCoordinate);

        public abstract Builder dependencies(Collection<MavenCoordinate> dependencies);

        public abstract Builder exports(Collection<MavenCoordinate> exports);

        public abstract Builder runtimeDependencies(
                Collection<MavenCoordinate> runtimeDependencies);

        public abstract Builder url(String url);

        public abstract Builder sourcesUrl(String sourcesUrl);

        public abstract Builder javadocUrl(String javadocUrl);

        public abstract Builder licenses(Collection<License> licenses);

        public abstract Dependency build();
    }
}
