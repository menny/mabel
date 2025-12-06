package net.evendanan.bazel.mvn.merger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;
import org.junit.Assert;
import org.junit.Test;

public class GraphUtilsTest {

  @Test
  public void testPrintGraphHappyPath() {
    final String simpleGraphPrintOut =
        ""
            + "  net.evendanan:dep1:0.1\n"
            + "    net.evendanan:inner1:0.1\n"
            + "      net.evendanan:inner-inner1:0.1\n"
            + "      net.evendanan:inner-inner2:0.1\n";

    Resolution resolution =
        Resolution.create(
            MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
            Arrays.asList(
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""))
                    .dependencies(
                        Collections.singleton(
                            MavenCoordinate.create("net.evendanan", "inner1", "0.1", "")))
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.1", ""))
                    .dependencies(
                        Arrays.asList(
                            MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""),
                            MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", "")))
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(
                        MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""))
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(
                        MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", ""))
                    .build()));

    final String actual = GraphUtils.printGraph(resolution);

    Assert.assertEquals(simpleGraphPrintOut, actual);
  }

  @Test
  public void testDoesNotTravelCircle() {
    Resolution resolution =
        Resolution.create(
            MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
            Arrays.asList(
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""))
                    .dependencies(
                        Collections.singleton(
                            MavenCoordinate.create("net.evendanan", "inner1", "0.1", "")))
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.1", ""))
                    .dependencies(
                        Arrays.asList(
                            MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""),
                            MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", "")))
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(
                        MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""))
                    .dependencies(
                        Collections.singletonList(
                            MavenCoordinate.create("net.evendanan", "inner1", "0.1", "")))
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(
                        MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", ""))
                    .build()));

    Set<MavenCoordinate> seen = new HashSet<>();

    GraphUtils.dfsTraveller(
        Collections.singleton(resolution),
        (dependency, integer) ->
            Assert.assertTrue(
                dependency.mavenCoordinate().toMavenString(),
                seen.add(dependency.mavenCoordinate())));
    Assert.assertEquals(4, seen.size());
  }
}
