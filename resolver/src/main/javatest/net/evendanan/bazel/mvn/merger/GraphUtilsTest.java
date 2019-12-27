package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Dependency;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class GraphUtilsTest {

    static final Collection<Dependency> GRAPH_WITH_EMPTY_REPO_AND_DIFFERENT_VERSIONS = Arrays.asList(
            Dependency.newBuilder()
                    .setGroupId("net.evendanan").setArtifactId("dep1").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(
                            Collections.singletonList(Dependency.newBuilder()
                                    .setGroupId("net.evendanan").setArtifactId("inner1").setVersion("0.1").setPackaging("jar")
                                    .addAllExports(Arrays.asList(
                                            Dependency.newBuilder()
                                                    .setGroupId("net.evendanan").setArtifactId("inner-inner1").setVersion("0.1").setPackaging("jar")
                                                    .setUrl("https://example.com/repo/artifact.jar")
                                                    .build(),
                                            Dependency.newBuilder()
                                                    .setGroupId("net.evendanan").setArtifactId("inner-inner2").setVersion("0.1").setPackaging("jar")
                                                    .build()))
                                    .setUrl("https://example.com/repo/artifact.jar").build()))
                    .setUrl("https://example.com/repo/artifact.jar")
                    .build(),

            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep2").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep3").setVersion("0.2").setPackaging("jar")
                                    .addAllDependencies(Arrays.asList(Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner-inner1").setVersion("0.2").setPackaging("jar")
                                                    .setUrl("https://example.com/repo/artifact.jar")
                                                    .build(),
                                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner-inner2").setVersion("0.1.1").setPackaging("jar")
                                                    .setUrl("https://example.com/repo/artifact.jar")
                                                    .build()))
                                    .setUrl("https://example.com/repo/artifact.jar").build()))
                    .addAllRuntimeDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner2").setVersion("0.1").setPackaging("jar")
                                    .setUrl("https://example.com/repo/artifact.jar")
                                    .build()))
                    .setUrl("http://example.com/artifact.jar")
                    .build());

    static final Collection<Dependency> NO_REPEATS_GRAPH = Arrays.asList(
            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep1").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner1").setVersion("0.1").setPackaging("jar")
                                    .addAllExports(
                                            Arrays.asList(Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner-inner1").setVersion("0.1").setPackaging("jar")
                                                            .setUrl("https://example.com/repo/artifact.jar").build(),
                                                    Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner-inner2").setVersion("0.1").setPackaging("jar")
                                                            .setUrl("https://example.com/repo/artifact.jar").build()))
                                    .setUrl("https://example.com/repo/artifact.jar")
                                    .build()))
                    .setUrl("https://example.com/repo/artifact.jar")
                    .build(),
            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep2").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep3").setVersion("0.2").setPackaging("jar")
                                    .setUrl("https://example.com/repo/artifact.jar")
                                    .build()))
                    .addAllRuntimeDependencies(
                            Collections.singletonList(
                                    Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner2").setVersion("0.1").setPackaging("jar")
                                            .setUrl("https://example.com/repo/artifact.jar")
                                            .build()))
                    .setUrl("http://example.com/artifact.jar").build());

    static final Collection<Dependency> SRC_JAR_GRAPH = Arrays.asList(
            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep1").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner1").setVersion("0.1").setPackaging("jar")
                                    .addAllExports(Arrays.asList(
                                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner-inner1").setVersion("0.1").setPackaging("jar")
                                                    .build(),
                                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner-inner2").setVersion("0.1").setPackaging("jar")
                                                    .build()))
                                    .setSourcesUrl("http://example.com/src.jar")
                                    .build()))
                    .build(),
            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep2").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep3").setVersion("0.2").setPackaging("jar")
                                    .build()))
                    .addAllRuntimeDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner2").setVersion("0.1").setPackaging("jar")
                                    .setUrl("http://example.com/artifact.jar")
                                    .setSourcesUrl("http://example.com/src.jar")
                                    .setJavadocUrl("http://example.com/doc.jar")
                                    .build()))
                    .setUrl("http://example.com/artifact.jar")
                    .build());

    static final Collection<Dependency> REPEATS_DEP1_AT_ROOT_GRAPH = Arrays.asList(
            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep1").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner1").setVersion("0.1").setPackaging("jar")
                                    .addAllExports(Arrays.asList(Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner-inner1").setVersion("0.1").setPackaging("jar")
                                                    .setUrl("http://example.com/artifact.jar")
                                                    .build(),
                                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner-inner2").setVersion("0.1").setPackaging("jar")
                                                    .setUrl("http://example.com/artifact.jar")
                                                    .build()))
                                    .setUrl("http://example.com/artifact.jar")
                                    .build()))
                    .setUrl("http://example.com/artifact.jar")
                    .build(),
            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep2").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep1").setVersion("0.2").setPackaging("jar")
                                    .setUrl("http://example.com/artifact.jar")
                                    .build()))
                    .addAllRuntimeDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner2").setVersion("0.1").setPackaging("jar")
                                    .setUrl("http://example.com/artifact.jar")
                                    .build()))
                    .setUrl("http://example.com/artifact.jar").build());

    static final Collection<Dependency> REPEATS_DEP6_AT_ROOT_GRAPH = Arrays.asList(
            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep1").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner1").setVersion("0.1").setPackaging("jar")
                                    .addAllExports(Arrays.asList(
                                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner-inner1").setVersion("0.1").setPackaging("jar")
                                                    .setUrl("http://example.com/artifact.jar")
                                                    .build(),
                                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep6").setVersion("0.0.1").setPackaging("jar")
                                                    .setUrl("http://example.com/artifact.jar")
                                                    .build()))
                                    .setUrl("http://example.com/artifact.jar")
                                    .build()))
                    .setUrl("http://example.com/artifact.jar")
                    .build(),
            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep2").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep1").setVersion("0.2").setPackaging("jar")
                                    .setUrl("http://example.com/artifact.jar").build()))
                    .addAllRuntimeDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner2").setVersion("0.1").setPackaging("jar")
                                    .setUrl("http://example.com/artifact.jar")
                                    .build()))
                    .setUrl("http://example.com/artifact.jar")
                    .build(),
            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep6").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("a1").setVersion("0.2").setPackaging("jar")
                                    .setUrl("http://example.com/artifact.jar")
                                    .build()))
                    .addAllRuntimeDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner-inner1").setVersion("0.4").setPackaging("jar")
                                    .setUrl("http://example.com/artifact.jar")
                                    .build()))
                    .setUrl("http://example.com/artifact.jar")
                    .build());

    static final Collection<Dependency> REPEATS_INNER1_GRAPH = Arrays.asList(
            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep1").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner1").setVersion("0.1").setPackaging("jar")
                                    .addAllExports(Arrays.asList(
                                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner-inner1").setVersion("0.1").setPackaging("jar")
                                                    .setUrl("http://example.com/artifact.jar")
                                                    .build(),
                                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner2").setVersion("0.1").setPackaging("jar")
                                                    .setUrl("http://example.com/artifact.jar")
                                                    .build()))
                                    .setUrl("http://example.com/artifact.jar")
                                    .build()))
                    .setUrl("http://example.com/artifact.jar")
                    .build(),
            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep2").setVersion("0.1").setPackaging("jar")
                    .addAllDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("dep3").setVersion("0.2").setPackaging("jar")
                                    .setUrl("http://example.com/artifact.jar")
                                    .build()))
                    .addAllRuntimeDependencies(Collections.singletonList(
                            Dependency.newBuilder().setGroupId("net.evendanan").setArtifactId("inner2").setVersion("0.3").setPackaging("jar")
                                    .setUrl("http://example.com/artifact.jar")
                                    .build()))
                    .setUrl("http://example.com/artifact.jar")
                    .build());

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