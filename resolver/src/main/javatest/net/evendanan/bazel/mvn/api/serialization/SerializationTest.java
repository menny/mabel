package net.evendanan.bazel.mvn.api.serialization;

import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;
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
        Resolution resolution = Resolution.create(
                MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""))
                                .dependencies(Collections.singleton(MavenCoordinate.create("net.evendanan", "inner1", "0.1", "")))
                                .url("http://example.com/artifact1.jar")
                                .sourcesUrl("http://example.com/artifact1-src.jar")
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.1", ""))
                                .url("http://example.com/artifact2.jar")
                                .dependencies(Arrays.asList(
                                        MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""),
                                        MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", "")
                                ))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""))
                                .url("http://example.com/artifact3.jar")
                                .javadocUrl("http://example.com/artifact3-javadoc.jar")
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", ""))
                                .url("http://example.com/artifact4.jar")
                                .sourcesUrl("http://example.com/artifact4-src.jar")
                                .build()));

        Serialization serialization = new Serialization();

        StringBuilder builder = new StringBuilder();
        serialization.serialize(resolution, builder);

        final Resolution actual = serialization.deserialize(new StringReader(builder.toString()));
        Assert.assertEquals(resolution.rootDependency(), actual.rootDependency());
        Assert.assertEquals(resolution.allResolvedDependencies().size(), actual.allResolvedDependencies().size());

        final List<Dependency> expectedDependencies = new ArrayList<>(resolution.allResolvedDependencies());
        final List<Dependency> actualDependencies = new ArrayList<>(actual.allResolvedDependencies());

        for (int depIndex = 0; depIndex < expectedDependencies.size(); depIndex++) {
            final Dependency expectedDep = expectedDependencies.get(depIndex);
            final Dependency actualDep = actualDependencies.get(depIndex);

            Assert.assertEquals(expectedDep.mavenCoordinate(), actualDep.mavenCoordinate());
            Assert.assertEquals(expectedDep.url(), actualDep.url());
            Assert.assertEquals(expectedDep.sourcesUrl(), actualDep.sourcesUrl());
            Assert.assertEquals(expectedDep.javadocUrl(), actualDep.javadocUrl());
            Assert.assertArrayEquals(expectedDep.licenses().toArray(), actualDep.licenses().toArray());
            Assert.assertArrayEquals(expectedDep.dependencies().toArray(), actualDep.dependencies().toArray());
            Assert.assertArrayEquals(expectedDep.exports().toArray(), actualDep.exports().toArray());
            Assert.assertArrayEquals(expectedDep.runtimeDependencies().toArray(), actualDep.runtimeDependencies().toArray());
        }
    }
}