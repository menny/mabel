package net.evendanan.bazel.mvn.merger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.evendanan.bazel.mvn.api.Dependency;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultMergerTest {

    @Before
    public void setup() {
    }

    @Test
    public void testExcludes_NoRemovalIfEmptyExclude() {
        String originalGraph = GraphUtils.printGraph(GraphUtilsTest.NO_REPEATS_GRAPH);

        Assert.assertEquals(originalGraph, GraphUtils.printGraph(ExcludesFilter.filterDependencies(GraphUtilsTest.NO_REPEATS_GRAPH, Collections.emptyList())));
    }

    @Test
    public void testExcludes_NoRemovalIfNoMatches() {
        String originalGraph = GraphUtils.printGraph(GraphUtilsTest.NO_REPEATS_GRAPH);

        Assert.assertEquals(originalGraph, GraphUtils.printGraph(ExcludesFilter.filterDependencies(GraphUtilsTest.NO_REPEATS_GRAPH, Arrays.asList("some:other:1.2", "net.evendanan:dep1:333.00", "net.evendanan:dep5"))));
    }

    @Test
    public void testExcludes_RemoveMatchesSubTree() {
        String originalGraph = GraphUtils.printGraph(GraphUtilsTest.NO_REPEATS_GRAPH);

        final String expectedOriginalGraph = "" +
                "  net.evendanan:dep1:0.1\n" +
                "    net.evendanan:inner1:0.1\n" +
                "      net.evendanan:inner-inner1:0.1\n" +
                "      net.evendanan:inner-inner2:0.1\n" +
                "  net.evendanan:dep2:0.1\n" +
                "    net.evendanan:dep3:0.2\n" +
                "    net.evendanan:inner2:0.1\n";

        Assert.assertEquals(expectedOriginalGraph, originalGraph);

        final String actual = GraphUtils.printGraph(ExcludesFilter.filterDependencies(GraphUtilsTest.NO_REPEATS_GRAPH, Collections.singletonList("net.evendanan:inner-inner2")));
        Assert.assertNotEquals(originalGraph, actual);

        final String expected = "" +
                "  net.evendanan:dep1:0.1\n" +
                "    net.evendanan:inner1:0.1\n" +
                "      net.evendanan:inner-inner1:0.1\n" +
                "  net.evendanan:dep2:0.1\n" +
                "    net.evendanan:dep3:0.2\n" +
                "    net.evendanan:inner2:0.1\n";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testReturnsUnChangedCopyWhenDependenciesDoNotAffectEachOther() {
        String expected = GraphUtils.printGraph(GraphUtilsTest.NO_REPEATS_GRAPH);

        PinBreadthFirstVersionsMerger merger = new PinBreadthFirstVersionsMerger();

        Collection<Dependency> mergedDependencies = merger.mergeGraphs(GraphUtilsTest.NO_REPEATS_GRAPH);

        Assert.assertEquals(expected, GraphUtils.printGraph(mergedDependencies));
    }

    @Test
    public void testPinsTopLevelVersion() {
        String expected = "" +
                "  net.evendanan:dep1:0.1\n" +
                "    net.evendanan:inner1:0.1\n" +
                "      net.evendanan:inner-inner1:0.1\n" +
                "      net.evendanan:inner-inner2:0.1\n" +
                "  net.evendanan:dep2:0.1\n" +
                "    net.evendanan:dep1:0.1\n" +
                "      net.evendanan:inner1:0.1\n" +
                "        net.evendanan:inner-inner1:0.1\n" +
                "        net.evendanan:inner-inner2:0.1\n" +
                "    net.evendanan:inner2:0.1\n";

        PinBreadthFirstVersionsMerger merger = new PinBreadthFirstVersionsMerger();

        Collection<Dependency> mergedDependencies = merger.mergeGraphs(GraphUtilsTest.REPEATS_DEP1_AT_ROOT_GRAPH);

        final String actual = GraphUtils.printGraph(mergedDependencies);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPinnedVersionsCheckerWhenNoRepeats() {
        final Collection<Dependency> dependencies = VerifyNoConflictingVersions.checkNoConflictingVersions(GraphUtilsTest.NO_REPEATS_GRAPH);
        Assert.assertSame(GraphUtilsTest.NO_REPEATS_GRAPH, dependencies);
    }

    @Test(expected = IllegalStateException.class)
    public void testPinnedVersionsCheckerWhenRepeats() {
        VerifyNoConflictingVersions.checkNoConflictingVersions(GraphUtilsTest.REPEATS_DEP6_AT_ROOT_GRAPH);
    }

    @Test
    public void testPinsFirstSeenVersion() {
        String expected = "" +
                "  net.evendanan:dep1:0.1\n" +
                "    net.evendanan:inner1:0.1\n" +
                "      net.evendanan:inner-inner1:0.1\n" +
                "      net.evendanan:inner2:0.3\n" +
                "  net.evendanan:dep2:0.1\n" +
                "    net.evendanan:dep3:0.2\n" +
                "    net.evendanan:inner2:0.3\n";

        PinBreadthFirstVersionsMerger merger = new PinBreadthFirstVersionsMerger();

        Collection<Dependency> mergedDependencies = merger.mergeGraphs(GraphUtilsTest.REPEATS_INNER1_GRAPH);

        final String actual = GraphUtils.printGraph(mergedDependencies);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFirstBreadthSeenVersion() {
        String expected = "" +
                "  net.evendanan:dep1:0.1\n" +
                "    net.evendanan:inner1:0.1\n" +
                "      net.evendanan:inner-inner1:0.4\n" +
                "      net.evendanan:dep6:0.1\n" +
                "        net.evendanan:a1:0.2\n" +
                "        net.evendanan:inner-inner1:0.4\n" +
                "  net.evendanan:dep2:0.1\n" +
                "    net.evendanan:dep1:0.1\n" +
                "      net.evendanan:inner1:0.1\n" +
                "        net.evendanan:inner-inner1:0.4\n" +
                "        net.evendanan:dep6:0.1\n" +
                "          net.evendanan:a1:0.2\n" +
                "          net.evendanan:inner-inner1:0.4\n" +
                "    net.evendanan:inner2:0.1\n" +
                "  net.evendanan:dep6:0.1\n" +
                "    net.evendanan:a1:0.2\n" +
                "    net.evendanan:inner-inner1:0.4\n";

        PinBreadthFirstVersionsMerger merger = new PinBreadthFirstVersionsMerger();

        Collection<Dependency> mergedDependencies = merger.mergeGraphs(GraphUtilsTest.REPEATS_DEP6_AT_ROOT_GRAPH);

        final String actual = GraphUtils.printGraph(mergedDependencies);
        Assert.assertEquals(expected, actual);
    }



    @Test
    public void testIgnoreEmptyRepository() {
        final String expected = "  net.evendanan:dep1:0.1\n" +
                "    net.evendanan:inner1:0.1\n" +
                "      net.evendanan:inner-inner1:0.1\n" +
                "      net.evendanan:inner-inner2:0.1.1\n" +
                "  net.evendanan:dep2:0.1\n" +
                "    net.evendanan:dep3:0.2\n" +
                "      net.evendanan:inner-inner1:0.1\n" +
                "      net.evendanan:inner-inner2:0.1.1\n" +
                "    net.evendanan:inner2:0.1\n";

        Collection<Dependency> mergedDependencies = new PinBreadthFirstVersionsMerger().mergeGraphs(GraphUtilsTest.GRAPH_WITH_EMPTY_REPO_AND_DIFFERENT_VERSIONS);

        Assert.assertEquals(expected, GraphUtils.printGraph(mergedDependencies));
    }

    @Test
    public void testReturnsUnchangedIfNoDuplicateDeps() {
        String expected = GraphUtils.printGraph(GraphUtilsTest.NO_REPEATS_GRAPH);

        FilterDuplicateDependenciesEntries merger = new FilterDuplicateDependenciesEntries();

        Collection<Dependency> dependencies = merger.filterDuplicateDependencies(GraphUtilsTest.NO_REPEATS_GRAPH);

        Assert.assertEquals(expected, GraphUtils.printGraph(dependencies));
    }

    @Test
    public void testReturnsDeDupDuplicateDeps() {
        List<Dependency> dependencies = (List<Dependency>) GraphUtils.deepCopyDeps(GraphUtilsTest.NO_REPEATS_GRAPH);

        //duplicating one root
        dependencies.add(dependencies.get(1));

        FilterDuplicateDependenciesEntries merger = new FilterDuplicateDependenciesEntries();

        Collection<Dependency> deDuped = merger.filterDuplicateDependencies(dependencies);
        String expected = GraphUtils.printGraph(GraphUtilsTest.NO_REPEATS_GRAPH);

        Assert.assertEquals(expected, GraphUtils.printGraph(deDuped));
    }

    @Test
    public void testNamePrefix() {
        AtomicInteger nodesCount = new AtomicInteger();

        GraphUtils.DfsTraveller(GraphUtilsTest.NO_REPEATS_GRAPH, ((dependency, integer) -> nodesCount.incrementAndGet()));
        Assert.assertTrue(nodesCount.get() > 0);

        final String prefix = "prefix___";
        final Collection<Dependency> dependencies = DependencyNamePrefixer.wrap(GraphUtilsTest.NO_REPEATS_GRAPH, prefix);

        GraphUtils.DfsTraveller(dependencies, ((dependency, integer) -> nodesCount.decrementAndGet()));
        Assert.assertEquals(0, nodesCount.get());

        GraphUtils.DfsTraveller(dependencies, ((dependency, integer) -> {
            Assert.assertTrue(dependency.repositoryRuleName().startsWith(prefix));
            Assert.assertTrue(dependency.targetName().startsWith(prefix));
        }));
    }

    @Test
    public void testFlattenTreeWithNoRepeats() {
        final ArrayList<Dependency> flatten = new ArrayList<>(DependencyTreeFlatter.flatten(GraphUtilsTest.NO_REPEATS_GRAPH));
        Assert.assertEquals(7, flatten.size());
        Assert.assertEquals("net.evendanan:dep1:0.1", flatten.get(0).mavenCoordinates());
        Assert.assertEquals("net.evendanan:inner1:0.1", flatten.get(1).mavenCoordinates());
        Assert.assertEquals("net.evendanan:inner-inner1:0.1", flatten.get(2).mavenCoordinates());
        Assert.assertEquals("net.evendanan:inner-inner2:0.1", flatten.get(3).mavenCoordinates());
        Assert.assertEquals("net.evendanan:dep2:0.1", flatten.get(4).mavenCoordinates());
        Assert.assertEquals("net.evendanan:dep3:0.2", flatten.get(5).mavenCoordinates());
        Assert.assertEquals("net.evendanan:inner2:0.1", flatten.get(6).mavenCoordinates());

    }

    @Test
    public void testClearSrcJar() {
        Assert.assertNotEquals("", ((List<Dependency>) ((List<Dependency>) GraphUtilsTest.SRC_JAR_GRAPH).get(0).dependencies()).get(0).sourcesUrl().toASCIIString());
        Assert.assertNotEquals("", ((List<Dependency>) ((List<Dependency>) GraphUtilsTest.SRC_JAR_GRAPH).get(1).runtimeDependencies()).get(0).url().toASCIIString());
        Assert.assertNotEquals("", ((List<Dependency>) ((List<Dependency>) GraphUtilsTest.SRC_JAR_GRAPH).get(1).runtimeDependencies()).get(0).sourcesUrl().toASCIIString());
        Assert.assertNotEquals("", ((List<Dependency>) ((List<Dependency>) GraphUtilsTest.SRC_JAR_GRAPH).get(1).runtimeDependencies()).get(0).javadocUrl().toASCIIString());
        final ArrayList<Dependency> cleared = new ArrayList<>(ClearSrcJarAttribute.clearSrcJar(GraphUtilsTest.SRC_JAR_GRAPH));
        Assert.assertEquals(2, cleared.size());
        GraphUtils.DfsTraveller(cleared, (dep, level) -> Assert.assertEquals("", dep.sourcesUrl().toASCIIString()));

        Assert.assertEquals("", ((List<Dependency>) cleared.get(0).dependencies()).get(0).sourcesUrl().toASCIIString());
        Assert.assertNotEquals("", ((List<Dependency>) cleared.get(1).runtimeDependencies()).get(0).url().toASCIIString());
        Assert.assertEquals("", ((List<Dependency>) cleared.get(1).runtimeDependencies()).get(0).sourcesUrl().toASCIIString());
        Assert.assertNotEquals("", ((List<Dependency>) cleared.get(1).runtimeDependencies()).get(0).javadocUrl().toASCIIString());
    }

    @Test
    public void testFlattenWithRepeats() {
        final ArrayList<Dependency> dependencies = new ArrayList<>(GraphUtils.deepCopyDeps(GraphUtilsTest.REPEATS_DEP6_AT_ROOT_GRAPH));
        dependencies.add(dependencies.get(1));

        final ArrayList<Dependency> flatten = new ArrayList<>(DependencyTreeFlatter.flatten(dependencies));

        final int expectedSize = 10;
        Assert.assertEquals(expectedSize, flatten.size());
        int flatDepIndex = 0;
        Assert.assertEquals("net.evendanan:dep1:0.1", flatten.get(flatDepIndex++).mavenCoordinates());
        Assert.assertEquals("net.evendanan:inner1:0.1", flatten.get(flatDepIndex++).mavenCoordinates());
        Assert.assertEquals("net.evendanan:inner-inner1:0.1", flatten.get(flatDepIndex++).mavenCoordinates());
        Assert.assertEquals("net.evendanan:dep6:0.0.1", flatten.get(flatDepIndex++).mavenCoordinates());
        Assert.assertEquals("net.evendanan:dep2:0.1", flatten.get(flatDepIndex++).mavenCoordinates());
        Assert.assertEquals("net.evendanan:dep1:0.2", flatten.get(flatDepIndex++).mavenCoordinates());
        Assert.assertEquals("net.evendanan:inner2:0.1", flatten.get(flatDepIndex++).mavenCoordinates());
        Assert.assertEquals("net.evendanan:dep6:0.1", flatten.get(flatDepIndex++).mavenCoordinates());
        Assert.assertEquals("net.evendanan:a1:0.2", flatten.get(flatDepIndex++).mavenCoordinates());
        Assert.assertEquals("net.evendanan:inner-inner1:0.4", flatten.get(flatDepIndex++).mavenCoordinates());

        Assert.assertEquals(expectedSize, flatDepIndex);
    }
}