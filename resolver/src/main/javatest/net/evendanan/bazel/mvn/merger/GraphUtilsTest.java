package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.MavenCoordinate;
import net.evendanan.bazel.mvn.api.Resolution;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class GraphUtilsTest {

    @Test
    public void testPrintGraphHappyPath() {
        final String simpleGraphPrintOut = "" +
                "  net.evendanan:dep1:0.1\n" +
                "    net.evendanan:inner1:0.1\n" +
                "      net.evendanan:inner-inner1:0.1\n" +
                "      net.evendanan:inner-inner2:0.1\n";

        Resolution resolution = Resolution.newBuilder()
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

        final String actual = GraphUtils.printGraph(resolution);

        Assert.assertEquals(simpleGraphPrintOut, actual);
    }
}