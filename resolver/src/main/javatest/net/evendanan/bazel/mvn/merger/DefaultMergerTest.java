package net.evendanan.bazel.mvn.merger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

        Collection<Dependency> mergedDependencies = merger.mergeGraphs(GraphUtilsTest.NO_REPEATS_GRAPH, Collections.emptyList());

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

        Collection<Dependency> mergedDependencies = merger.mergeGraphs(GraphUtilsTest.REPEATS_DEP1_AT_ROOT_GRAPH, Collections.emptyList());

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

        Collection<Dependency> mergedDependencies = merger.mergeGraphs(GraphUtilsTest.REPEATS_INNER1_GRAPH, Collections.emptyList());

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

        Collection<Dependency> mergedDependencies = merger.mergeGraphs(GraphUtilsTest.REPEATS_DEP6_AT_ROOT_GRAPH, Collections.emptyList());

        final String actual = GraphUtils.printGraph(mergedDependencies);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testReturnsUnchangedIfNoDuplicateDeps() {
        String expected = GraphUtils.printGraph(GraphUtilsTest.NO_REPEATS_GRAPH);

        DuplicatesDepsRemovingMerger merger = new DuplicatesDepsRemovingMerger();

        Collection<Dependency> dependencies = merger.mergeGraphs(GraphUtilsTest.NO_REPEATS_GRAPH, Collections.emptyList());

        Assert.assertEquals(expected, GraphUtils.printGraph(dependencies));
    }

    @Test
    public void testReturnsDeDupDuplicateDeps() {
        List<Dependency> dependencies = (List<Dependency>)GraphUtils.deepCopyDeps(GraphUtilsTest.NO_REPEATS_GRAPH);

        //duplicating one root
        dependencies.add(dependencies.get(1));

        DuplicatesDepsRemovingMerger merger = new DuplicatesDepsRemovingMerger();

        Collection<Dependency> deDuped = merger.mergeGraphs(dependencies, Collections.emptyList());
        String expected = GraphUtils.printGraph(GraphUtilsTest.NO_REPEATS_GRAPH);

        Assert.assertEquals(expected, GraphUtils.printGraph(deDuped));
    }
}