package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class GraphVerificationsTest {

    private Resolution mBasicResolution;

    @Before
    public void setup() {
        mBasicResolution =
                Resolution.create(
                        MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
                        Arrays.asList(
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "dep1", "0.1", ""))
                                        .url("https://example.com/dep1.jar")
                                        .dependencies(
                                                Collections.singleton(
                                                        MavenCoordinate.create(
                                                                "net.evendanan",
                                                                "inner1",
                                                                "0.1",
                                                                "")))
                                        .build(),
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner1", "0.1", ""))
                                        .dependencies(
                                                Arrays.asList(
                                                        MavenCoordinate.create(
                                                                "net.evendanan",
                                                                "inner-inner1",
                                                                "0.1",
                                                                ""),
                                                        MavenCoordinate.create(
                                                                "net.evendanan",
                                                                "inner-inner2",
                                                                "0.1",
                                                                "")))
                                        .url("https://example.com/dep1.jar")
                                        .build(),
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner-inner1", "0.1", ""))
                                        .url("https://example.com/dep1.jar")
                                        .build(),
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner-inner2", "0.1", ""))
                                        .url("https://example.com/dep1.jar")
                                        .build()));
    }

    @Test
    public void checkNoConflictingVersions_HappyPath() {
        GraphVerifications.checkNoConflictingVersions(mBasicResolution.allResolvedDependencies());
    }

    @Test(expected = GraphVerifications.InvalidGraphException.class)
    public void checkNoConflictingVersions_Fail() {
        GraphVerifications.checkNoConflictingVersions(
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "inner1", "0.1", ""))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "inner-inner1", "0.1", ""))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "inner-inner1", "0.2", ""))
                                .build()));
    }

    @Test
    public void checkAllGraphDependenciesAreResolved_HappyPath() {
        GraphVerifications.checkAllGraphDependenciesAreResolved(mBasicResolution);
    }

    @Test(expected = NullPointerException.class)
    public void checkAllGraphDependenciesAreResolved_FailRootMissing() {
        GraphVerifications.checkAllGraphDependenciesAreResolved(
                Resolution.create(
                        MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
                        Arrays.asList(
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner1", "0.1", ""))
                                        .dependencies(
                                                Arrays.asList(
                                                        MavenCoordinate.create(
                                                                "net.evendanan",
                                                                "inner-inner1",
                                                                "0.1",
                                                                ""),
                                                        MavenCoordinate.create(
                                                                "net.evendanan",
                                                                "inner-inner2",
                                                                "0.1",
                                                                "")))
                                        .build(),
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner-inner1", "0.1", ""))
                                        .build(),
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner-inner2", "0.1", ""))
                                        .build())));
    }

    @Test(expected = NullPointerException.class)
    public void checkAllGraphDependenciesAreResolved_FailDependencyMissing() {
        GraphVerifications.checkAllGraphDependenciesAreResolved(
                Resolution.create(
                        MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
                        Arrays.asList(
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "dep1", "0.1", ""))
                                        .dependencies(
                                                Collections.singleton(
                                                        MavenCoordinate.create(
                                                                "net.evendanan",
                                                                "inner1",
                                                                "0.1",
                                                                "")))
                                        .build(),
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner1", "0.1", ""))
                                        .dependencies(
                                                Arrays.asList(
                                                        MavenCoordinate.create(
                                                                "net.evendanan",
                                                                "inner-inner1",
                                                                "0.1",
                                                                ""),
                                                        MavenCoordinate.create(
                                                                "net.evendanan",
                                                                "inner-inner2",
                                                                "0.1",
                                                                "")))
                                        .build(),
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner-inner1", "0.1", ""))
                                        .build())));
    }

    @Test
    public void checkGraphDoesNotHaveDanglingDependencies_HappyPath() {
        GraphVerifications.checkGraphDoesNotHaveDanglingDependencies(mBasicResolution);
    }

    @Test(expected = GraphVerifications.InvalidGraphException.class)
    public void checkGraphDoesNotHaveDanglingDependencies_Fail() {
        GraphVerifications.checkGraphDoesNotHaveDanglingDependencies(
                Resolution.create(
                        MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
                        Arrays.asList(
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "dep1", "0.1", ""))
                                        .dependencies(
                                                Collections.singleton(
                                                        MavenCoordinate.create(
                                                                "net.evendanan",
                                                                "inner1",
                                                                "0.1",
                                                                "")))
                                        .build(),
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner1", "0.1", ""))
                                        .dependencies(
                                                Arrays.asList(
                                                        MavenCoordinate.create(
                                                                "net.evendanan",
                                                                "inner-inner1",
                                                                "0.1",
                                                                ""),
                                                        MavenCoordinate.create(
                                                                "net.evendanan",
                                                                "inner-inner2",
                                                                "0.1",
                                                                "")))
                                        .build(),
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner-inner1", "0.1", ""))
                                        .build(),
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner-inner2", "0.1", ""))
                                        .build(),
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan",
                                                        "inner-inner3" /*dangling*/,
                                                        "0.1",
                                                        ""))
                                        .build())));
    }

    @Test
    public void checkAllDependenciesAreResolved_HappyPath() {
        GraphVerifications.checkAllDependenciesAreResolved(
                mBasicResolution.allResolvedDependencies());
    }

    @Test(expected = GraphVerifications.InvalidGraphException.class)
    public void checkAllDependenciesAreResolved_Fail() {
        GraphVerifications.checkAllDependenciesAreResolved(
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""))
                                .dependencies(
                                        Collections.singleton(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner1", "0.1", "")))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "inner1", "0.1", ""))
                                .dependencies(
                                        Arrays.asList(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner-inner1", "0.1", ""),
                                                MavenCoordinate.create(
                                                        "net.evendanan",
                                                        "inner-inner2" /*this is missing*/,
                                                        "0.1",
                                                        "")))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "inner-inner1", "0.1", ""))
                                .build()));
    }

    @Test
    public void checkNoRepeatingDependencies_HappyPath() {
        GraphVerifications.checkNoRepeatingDependencies(mBasicResolution.allResolvedDependencies());
    }

    @Test(expected = GraphVerifications.InvalidGraphException.class)
    public void checkNoRepeatingDependencies_Fail() {
        GraphVerifications.checkNoRepeatingDependencies(
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""))
                                .dependencies(
                                        Collections.singleton(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner1", "0.1", "")))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "inner1", "0.1", ""))
                                .dependencies(
                                        Arrays.asList(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "inner-inner1", "0.1", ""),
                                                MavenCoordinate.create(
                                                        "net.evendanan",
                                                        "inner-inner2",
                                                        "0.1",
                                                        "")))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "inner-inner1", "0.1", ""))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan",
                                                "inner-inner1" /*repeating*/,
                                                "0.1",
                                                ""))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "inner-inner2", "0.1", ""))
                                .build()));
    }

    @Test
    public void testRootResolved_HappyPath() {
        GraphVerifications.checkResolutionSuccess(mBasicResolution);
    }

    @Test(expected = GraphVerifications.InvalidGraphException.class)
    public void testRootResolved_MissingUrl() {
        final MavenCoordinate root = MavenCoordinate.create("net.evendanan", "dep1", "0.1", "");
        Resolution resolution =
                Resolution.create(
                        root,
                        Collections.singletonList(
                                Dependency.builder()
                                        .mavenCoordinate(root)
                                        .build()));
        GraphVerifications.checkResolutionSuccess(resolution);
    }

}
