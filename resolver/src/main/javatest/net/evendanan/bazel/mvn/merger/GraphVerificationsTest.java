package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.MavenCoordinate;
import net.evendanan.bazel.mvn.api.Resolution;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class GraphVerificationsTest {

    private Resolution mBasicResolution;

    @Before
    public void setup() {
        mBasicResolution = Resolution.newBuilder()
                .setRootDependency(MavenCoordinate.newBuilder()
                        .setGroupId("net.evendanan")
                        .setArtifactId("dep1")
                        .setVersion("0.1")
                        .build())
                .addAllAllResolvedDependencies(Arrays.asList(
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("dep1")
                                        .setVersion("0.1")
                                        .build())
                                .addAllDependencies(Collections.singleton(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.1")
                                        .build()))
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.1")
                                        .build())
                                .addAllDependencies(Arrays.asList(
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner1")
                                                .setVersion("0.1")
                                                .build(),
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner2")
                                                .setVersion("0.1")
                                                .build()
                                ))
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner-inner1")
                                        .setVersion("0.1")
                                        .build())
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner-inner2")
                                        .setVersion("0.1")
                                        .build())
                                .build()))
                .build();
    }

    @Test
    public void checkNoConflictingVersions_HappyPath() {
        GraphVerifications.checkNoConflictingVersions(mBasicResolution.getAllResolvedDependenciesList());
    }

    @Test(expected = IllegalStateException.class)
    public void checkNoConflictingVersions_Fail() {
        GraphVerifications.checkNoConflictingVersions(Arrays.asList(
                Dependency.newBuilder()
                        .setMavenCoordinate(MavenCoordinate.newBuilder()
                                .setGroupId("net.evendanan")
                                .setArtifactId("dep1")
                                .setVersion("0.1")
                                .build())
                        .build(),
                Dependency.newBuilder()
                        .setMavenCoordinate(MavenCoordinate.newBuilder()
                                .setGroupId("net.evendanan")
                                .setArtifactId("inner1")
                                .setVersion("0.1")
                                .build())
                        .build(),
                Dependency.newBuilder()
                        .setMavenCoordinate(MavenCoordinate.newBuilder()
                                .setGroupId("net.evendanan")
                                .setArtifactId("inner-inner1")
                                .setVersion("0.1")
                                .build())
                        .build(),
                Dependency.newBuilder()
                        .setMavenCoordinate(MavenCoordinate.newBuilder()
                                .setGroupId("net.evendanan")
                                .setArtifactId("inner-inner1")
                                .setVersion("0.2")
                                .build())
                        .build()));
    }

    /*
    (final Collection<Dependency> dependencies) {
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
     */
}