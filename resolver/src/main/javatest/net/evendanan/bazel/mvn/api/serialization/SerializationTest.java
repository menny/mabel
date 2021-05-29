package net.evendanan.bazel.mvn.api.serialization;

import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.License;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;
import net.evendanan.bazel.mvn.api.model.ResolutionOutput;
import net.evendanan.bazel.mvn.api.model.TargetType;

import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SerializationTest {

    @Test
    public void testHappyPath() {
        final Resolution resolution = Resolution.create(
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
                                .url("http://example.com/artifact1.jar")
                                .sourcesUrl("http://example.com/artifact1-src.jar")
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "inner1", "0.1", ""))
                                .url("http://example.com/artifact2.jar")
                                .licenses(Collections.singletonList(License.create("Apache-2", "http://example.com")))
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
                                .url("http://example.com/artifact3.jar")
                                .javadocUrl("http://example.com/artifact3-javadoc.jar")
                                .licenses(Collections.singletonList(License.create("Apache-2", "")))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "inner-inner2", "0.1", ""))
                                .licenses(Arrays.asList(
                                        License.create("Weird", ""),
                                        License.create("BSD \"NEW\"", "http://example.com/bsd.html")))
                                .url("http://example.com/artifact4.jar")
                                .sourcesUrl("http://example.com/artifact4-src.jar")
                                .build()));
        final ResolutionOutput resolutionOutput =
                ResolutionOutput.create(
                        TargetType.auto,
                        false,
                        resolution);

        Serialization serialization = new Serialization();

        StringBuilder builder = new StringBuilder();
        serialization.serialize(resolutionOutput, builder);

        final ResolutionOutput actualOutput = serialization.deserialize(new StringReader(builder.toString()));
        Assert.assertEquals(resolutionOutput.targetType(), actualOutput.targetType());
        Assert.assertEquals(resolutionOutput.testOnly(), actualOutput.testOnly());

        final Resolution actual = actualOutput.resolution();
        Assert.assertEquals(resolution.rootDependency(), actual.rootDependency());
        Assert.assertEquals(
                resolution.allResolvedDependencies().size(),
                actual.allResolvedDependencies().size());

        final List<Dependency> expectedDependencies =
                new ArrayList<>(resolution.allResolvedDependencies());
        final List<Dependency> actualDependencies =
                new ArrayList<>(actual.allResolvedDependencies());

        for (int depIndex = 0; depIndex < expectedDependencies.size(); depIndex++) {
            final Dependency expectedDep = expectedDependencies.get(depIndex);
            final Dependency actualDep = actualDependencies.get(depIndex);

            Assert.assertEquals(expectedDep.mavenCoordinate(), actualDep.mavenCoordinate());
            Assert.assertEquals(expectedDep.url(), actualDep.url());
            Assert.assertEquals(expectedDep.sourcesUrl(), actualDep.sourcesUrl());
            Assert.assertEquals(expectedDep.javadocUrl(), actualDep.javadocUrl());
            Assert.assertArrayEquals(
                    expectedDep.licenses().toArray(), actualDep.licenses().toArray());
            Assert.assertArrayEquals(
                    expectedDep.dependencies().toArray(), actualDep.dependencies().toArray());
            Assert.assertArrayEquals(
                    expectedDep.exports().toArray(), actualDep.exports().toArray());
            Assert.assertArrayEquals(
                    expectedDep.runtimeDependencies().toArray(),
                    actualDep.runtimeDependencies().toArray());
        }
    }
}
