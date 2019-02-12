package net.evendanan.bazel.mvn.merger;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.evendanan.bazel.mvn.api.Dependency;
import org.junit.Assert;
import org.junit.Test;

public class GraphUtilsTest {

    static final Collection<Dependency> GRAPH_WITH_EMPTY_REPO_AND_DIFFERENT_VERSIONS = Arrays.asList(
            new Dependency("net.evendanan", "dep1", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "inner1", "0.1", "jar",
                            Collections.emptyList(),
                            Arrays.asList(new Dependency("net.evendanan", "inner-inner1", "0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()),
                                    new Dependency("net.evendanan", "inner-inner2", "0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList())),
                            Collections.emptyList(), URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(), Collections.emptyList(), URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()),
            new Dependency("net.evendanan", "dep2", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "dep3", "0.2", "jar",
                            Arrays.asList(new Dependency("net.evendanan", "inner-inner1", "0.2", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()),
                                    new Dependency("net.evendanan", "inner-inner2", "0.1.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())), Collections.emptyList(), Collections.emptyList(),
                            URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(),
                    Collections.singletonList(new Dependency("net.evendanan", "inner2", "0.1", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()));

    static final Collection<Dependency> NO_REPEATS_GRAPH = Arrays.asList(
            new Dependency("net.evendanan", "dep1", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "inner1", "0.1", "jar",
                            Collections.emptyList(),
                            Arrays.asList(new Dependency("net.evendanan", "inner-inner1", "0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()),
                                    new Dependency("net.evendanan", "inner-inner2", "0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                            Collections.emptyList(), URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(), Collections.emptyList(), URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()),
            new Dependency("net.evendanan", "dep2", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "dep3", "0.2", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(),
                    Collections.singletonList(new Dependency("net.evendanan", "inner2", "0.1", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("https://example.com/repo/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()));

    static final Collection<Dependency> SRC_JAR_GRAPH = Arrays.asList(
            new Dependency("net.evendanan", "dep1", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "inner1", "0.1", "jar",
                            Collections.emptyList(),
                            Arrays.asList(new Dependency("net.evendanan", "inner-inner1", "0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList()),
                                    new Dependency("net.evendanan", "inner-inner2", "0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList())),
                            Collections.emptyList(), URI.create(""), URI.create("http://example.com/src.jar"), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(), Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList()),
            new Dependency("net.evendanan", "dep2", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "dep3", "0.2", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create(""), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(),
                    Collections.singletonList(new Dependency("net.evendanan", "inner2", "0.1", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create("http://example.com/src.jar"), URI.create("http://example.com/doc.jar"), Collections.emptyList())), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()));

    static final Collection<Dependency> REPEATS_DEP1_AT_ROOT_GRAPH = Arrays.asList(
            new Dependency("net.evendanan", "dep1", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "inner1", "0.1", "jar",
                            Collections.emptyList(),
                            Arrays.asList(new Dependency("net.evendanan", "inner-inner1", "0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()),
                                    new Dependency("net.evendanan", "inner-inner2", "0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                            Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()),
            new Dependency("net.evendanan", "dep2", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "dep1", "0.2", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(),
                    Collections.singletonList(new Dependency("net.evendanan", "inner2", "0.1", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()));

    static final Collection<Dependency> REPEATS_DEP6_AT_ROOT_GRAPH = Arrays.asList(
            new Dependency("net.evendanan", "dep1", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "inner1", "0.1", "jar",
                            Collections.emptyList(),
                            Arrays.asList(new Dependency("net.evendanan", "inner-inner1", "0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()),
                                    new Dependency("net.evendanan", "dep6", "0.0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                            Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()),
            new Dependency("net.evendanan", "dep2", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "dep1", "0.2", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(),
                    Collections.singletonList(new Dependency("net.evendanan", "inner2", "0.1", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()),
            new Dependency("net.evendanan", "dep6", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "a1", "0.2", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(),
                    Collections.singletonList(new Dependency("net.evendanan", "inner-inner1", "0.4", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()));

    static final Collection<Dependency> REPEATS_INNER1_GRAPH = Arrays.asList(
            new Dependency("net.evendanan", "dep1", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "inner1", "0.1", "jar",
                            Collections.emptyList(),
                            Arrays.asList(new Dependency("net.evendanan", "inner-inner1", "0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()),
                                    new Dependency("net.evendanan", "inner2", "0.1", "jar",
                                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                            Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()),
            new Dependency("net.evendanan", "dep2", "0.1", "jar",
                    Collections.singletonList(new Dependency("net.evendanan", "dep3", "0.2", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())),
                    Collections.emptyList(),
                    Collections.singletonList(new Dependency("net.evendanan", "inner2", "0.3", "jar",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList())), URI.create("http://example.com/artifact.jar"), URI.create(""), URI.create(""), Collections.emptyList()));

    @Test
    public void testPrintGraph_NO_REPEATS_GRAPH() {
        final String simpleGraphPrintOut = "" +
                "  net.evendanan:dep1:0.1\n" +
                "    net.evendanan:inner1:0.1\n" +
                "      net.evendanan:inner-inner1:0.1\n" +
                "      net.evendanan:inner-inner2:0.1\n" +
                "  net.evendanan:dep2:0.1\n" +
                "    net.evendanan:dep3:0.2\n" +
                "    net.evendanan:inner2:0.1\n";

        final String actual = GraphUtils.printGraph(NO_REPEATS_GRAPH);

        Assert.assertEquals(simpleGraphPrintOut, actual);
    }

    @Test
    public void testPrintGraph_REPEATS_DEP1_AT_ROOT_GRAPH() {
        final String simpleGraphPrintOut = "" +
                "  net.evendanan:dep1:0.1\n" +
                "    net.evendanan:inner1:0.1\n" +
                "      net.evendanan:inner-inner1:0.1\n" +
                "      net.evendanan:inner-inner2:0.1\n" +
                "  net.evendanan:dep2:0.1\n" +
                "    net.evendanan:dep1:0.2\n" +
                "    net.evendanan:inner2:0.1\n";

        final String actual = GraphUtils.printGraph(REPEATS_DEP1_AT_ROOT_GRAPH);
        Assert.assertEquals(simpleGraphPrintOut, actual);
    }

    @Test
    public void testPrintGraph_REPEATS_INNER1_GRAPH() {
        final String simpleGraphPrintOut = "" +
                "  net.evendanan:dep1:0.1\n" +
                "    net.evendanan:inner1:0.1\n" +
                "      net.evendanan:inner-inner1:0.1\n" +
                "      net.evendanan:inner2:0.1\n" +
                "  net.evendanan:dep2:0.1\n" +
                "    net.evendanan:dep3:0.2\n" +
                "    net.evendanan:inner2:0.3\n";

        final String actual = GraphUtils.printGraph(REPEATS_INNER1_GRAPH);
        Assert.assertEquals(simpleGraphPrintOut, actual);
    }

    @Test
    public void testPrintGraph_REPEATS_DEP6_AT_ROOT_GRAPH() {
        final String simpleGraphPrintOut = "" +
                "  net.evendanan:dep1:0.1\n" +
                "    net.evendanan:inner1:0.1\n" +
                "      net.evendanan:inner-inner1:0.1\n" +
                "      net.evendanan:dep6:0.0.1\n" +
                "  net.evendanan:dep2:0.1\n" +
                "    net.evendanan:dep1:0.2\n" +
                "    net.evendanan:inner2:0.1\n" +
                "  net.evendanan:dep6:0.1\n" +
                "    net.evendanan:a1:0.2\n" +
                "    net.evendanan:inner-inner1:0.4\n";

        final String actual = GraphUtils.printGraph(REPEATS_DEP6_AT_ROOT_GRAPH);
        Assert.assertEquals(simpleGraphPrintOut, actual);
    }
}