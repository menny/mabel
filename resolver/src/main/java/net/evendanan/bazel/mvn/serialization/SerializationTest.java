package net.evendanan.bazel.mvn.serialization;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.evendanan.bazel.mvn.api.Dependency;
import org.junit.Assert;
import org.junit.Test;

public class SerializationTest {

    private static final Collection<Dependency> REPEATS_DEP6_AT_ROOT_GRAPH = Arrays.asList(
            new Dependency("net.evendanan", "dep1", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "inner1", "0.1", "jar",
                            Collections.emptyList(),
                            Arrays.asList(new Dependency("net.evendanan", "inner-inner1", "0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList()),
                                    new Dependency("net.evendanan", "dep6", "0.0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList())),
                            Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(), Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList()),
            new Dependency("net.evendanan", "dep2", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "dep1", "0.2", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(),
                    Collections.singletonList(new Dependency("net.evendanan", "inner2", "0.1", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList())), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList()),
            new Dependency("net.evendanan", "dep6", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "a1", "0.2", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(),
                    Collections.singletonList(new Dependency("net.evendanan", "inner-inner1", "0.4", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList())), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList()));

    private static void assertEqualsList(Collection<Dependency> expectedCollection, Collection<Dependency> actualCollection) {
        final ArrayList<Dependency> expectedList = new ArrayList<>(expectedCollection);
        final ArrayList<Dependency> actualList = new ArrayList<>(actualCollection);

        Assert.assertEquals(expectedList.size(), actualList.size());

        for (int expectedElementIndex = 0; expectedElementIndex < expectedList.size(); expectedElementIndex++) {
            final Dependency expectedDependency = expectedList.get(expectedElementIndex);
            final Dependency actualDependency = actualList.get(expectedElementIndex);

            Assert.assertEquals("index " + expectedElementIndex, expectedDependency, actualDependency);

            Assert.assertEquals(expectedDependency.packaging(), actualDependency.packaging());
            Assert.assertEquals(expectedDependency.url(), actualDependency.url());
            Assert.assertEquals(expectedDependency.javadocUrl(), actualDependency.javadocUrl());
            Assert.assertEquals(expectedDependency.sourcesUrl(), actualDependency.sourcesUrl());

            Assert.assertArrayEquals(expectedDependency.licenses().toArray(), actualDependency.licenses().toArray());

            assertEqualsList(expectedDependency.dependencies(), actualDependency.dependencies());
            assertEqualsList(expectedDependency.exports(), actualDependency.exports());
            assertEqualsList(expectedDependency.runtimeDependencies(), actualDependency.runtimeDependencies());
        }
    }

    @Test
    public void testHappyPath() {
        Serialization serialization = new Serialization();

        final String serialized = serialization.serialize(REPEATS_DEP6_AT_ROOT_GRAPH);

        final List<Dependency> deserialized = serialization.deserialize(serialized);

        assertEqualsList(REPEATS_DEP6_AT_ROOT_GRAPH, deserialized);
    }
}
