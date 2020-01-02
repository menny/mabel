package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.MavenCoordinate;
import net.evendanan.bazel.mvn.api.Resolution;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

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

    @Test(expected = GraphVerifications.InvalidGraphException.class)
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

    @Test
    public void checkAllGraphDependenciesAreResolved_HappyPath() {
        GraphVerifications.checkAllGraphDependenciesAreResolved(mBasicResolution);
    }

    @Test(expected = NullPointerException.class)
    public void checkAllGraphDependenciesAreResolved_FailRootMissing() {
        GraphVerifications.checkAllGraphDependenciesAreResolved(Resolution.newBuilder()
                .setRootDependency(MavenCoordinate.newBuilder()
                        .setGroupId("net.evendanan")
                        .setArtifactId("dep1")
                        .setVersion("0.1")
                        .build())
                .addAllAllResolvedDependencies(Arrays.asList(
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
                .build());
    }

    @Test(expected = NullPointerException.class)
    public void checkAllGraphDependenciesAreResolved_FailDependencyMissing() {
        GraphVerifications.checkAllGraphDependenciesAreResolved(Resolution.newBuilder()
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
                                .build()))
                .build());
    }

    @Test
    public void checkGraphDoesNotHaveDanglingDependencies_HappyPath() {
        GraphVerifications.checkGraphDoesNotHaveDanglingDependencies(mBasicResolution);
    }

    @Test(expected = GraphVerifications.InvalidGraphException.class)
    public void checkGraphDoesNotHaveDanglingDependencies_Fail() {
        GraphVerifications.checkGraphDoesNotHaveDanglingDependencies(Resolution.newBuilder()
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
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner-inner3")//dangling
                                        .setVersion("0.1")
                                        .build())
                                .build()))
                .build());
    }

    @Test
    public void checkAllDependenciesAreResolved_HappyPath() {
        GraphVerifications.checkAllDependenciesAreResolved(mBasicResolution.getAllResolvedDependenciesList());
    }

    @Test(expected = GraphVerifications.InvalidGraphException.class)
    public void checkAllDependenciesAreResolved_Fail() {
        GraphVerifications.checkAllDependenciesAreResolved(Arrays.asList(
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
                                        .setArtifactId("inner-inner2")//this is missing
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
                        .build()));
    }

    @Test
    public void checkNoRepeatingDependencies_HappyPath() {
        GraphVerifications.checkNoRepeatingDependencies(mBasicResolution.getAllResolvedDependenciesList());
    }

    @Test(expected = GraphVerifications.InvalidGraphException.class)
    public void checkNoRepeatingDependencies_Fail() {
        GraphVerifications.checkNoRepeatingDependencies(Arrays.asList(
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
                                .setArtifactId("inner-inner1")//repeating
                                .setVersion("0.1")
                                .build())
                        .build(),
                Dependency.newBuilder()
                        .setMavenCoordinate(MavenCoordinate.newBuilder()
                                .setGroupId("net.evendanan")
                                .setArtifactId("inner-inner2")
                                .setVersion("0.1")
                                .build())
                        .build()));
    }
}