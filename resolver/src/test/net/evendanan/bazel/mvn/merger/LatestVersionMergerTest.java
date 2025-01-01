package net.evendanan.bazel.mvn.merger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LatestVersionMergerTest {

  private PinLatestVersionMerger mUnderTest;

  @Before
  public void setup() throws Exception {
    mUnderTest = new PinLatestVersionMerger();
  }

  @Test
  public void testHappyPath() throws Exception {
    Resolution root1 =
        Resolution.create(
            MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
            Arrays.asList(
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""))
                    .dependencies(
                        Collections.singleton(
                            MavenCoordinate.create("net.evendanan", "inner1", "0.1", "")))
                    .url("http://example.com/artifact1.jar")
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.1", ""))
                    .url("http://example.com/artifact2.jar")
                    .build()));
    Resolution root2 =
        Resolution.create(
            MavenCoordinate.create("net.evendanan", "dep2", "0.1", ""),
            Arrays.asList(
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "dep2", "0.1", ""))
                    .dependencies(
                        Collections.singleton(
                            MavenCoordinate.create("net.evendanan", "inner1", "0.2", "")))
                    .url("http://example.com/artifact1.jar")
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.2", ""))
                    .url("http://example.com/artifact2.jar")
                    .build()));

    Collection<Dependency> resolved = mUnderTest.mergeGraphs(Arrays.asList(root1, root2));
    Assert.assertEquals(3, resolved.size());
    Dependency dep1 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("dep1"))
            .findFirst()
            .orElseThrow(NullPointerException::new);
    Dependency dep2 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("dep2"))
            .findFirst()
            .orElseThrow(NullPointerException::new);
    Dependency inner1 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("inner1"))
            .findFirst()
            .orElseThrow(NullPointerException::new);

    Assert.assertTrue(
        dep1.dependencies().stream()
            .allMatch(d -> d.artifactId().equals("inner1") && d.version().equals("0.2")));
    Assert.assertTrue(
        dep2.dependencies().stream()
            .allMatch(d -> d.artifactId().equals("inner1") && d.version().equals("0.2")));
    Assert.assertEquals(0, inner1.dependencies().size());
  }

  @Test
  public void testPinRootVersion() throws Exception {
    Resolution root1 =
        Resolution.create(
            MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
            Arrays.asList(
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""))
                    .dependencies(
                        Collections.singleton(
                            MavenCoordinate.create("net.evendanan", "inner1", "0.1.1", "")))
                    .url("http://example.com/artifact1.jar")
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.1.1", ""))
                    .url("http://example.com/artifact2.jar")
                    .build()));
    Resolution root2 =
        Resolution.create(
            MavenCoordinate.create("net.evendanan", "dep2", "0.1", ""),
            Arrays.asList(
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "dep2", "0.1", ""))
                    .dependencies(
                        Collections.singleton(
                            MavenCoordinate.create("net.evendanan", "inner1", "0.2", "")))
                    .url("http://example.com/artifact1.jar")
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.2", ""))
                    .url("http://example.com/artifact2.jar")
                    .build()));
    Resolution root3 =
        Resolution.create(
            MavenCoordinate.create("net.evendanan", "inner1", "0.1", ""),
            Collections.singletonList(
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.1", ""))
                    .url("http://example.com/artifact111.jar")
                    .build()));

    Collection<Dependency> resolved = mUnderTest.mergeGraphs(Arrays.asList(root1, root2, root3));
    Assert.assertEquals(3, resolved.size());
    Dependency dep1 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("dep1"))
            .findFirst()
            .orElseThrow(NullPointerException::new);
    Dependency dep2 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("dep2"))
            .findFirst()
            .orElseThrow(NullPointerException::new);
    Dependency inner1 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("inner1"))
            .findFirst()
            .orElseThrow(NullPointerException::new);

    Assert.assertTrue(
        dep1.dependencies().stream()
            .peek(d -> System.out.println("dep1.dependencies(): " + d))
            .allMatch(d -> d.artifactId().equals("inner1") && d.version().equals("0.1")));
    Assert.assertTrue(
        dep2.dependencies().stream()
            .allMatch(d -> d.artifactId().equals("inner1") && d.version().equals("0.1")));
    Assert.assertEquals(0, inner1.dependencies().size());

    // different order
    resolved = mUnderTest.mergeGraphs(Arrays.asList(root3, root1, root2));
    Assert.assertEquals(3, resolved.size());
    dep1 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("dep1"))
            .findFirst()
            .orElseThrow(NullPointerException::new);
    dep2 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("dep2"))
            .findFirst()
            .orElseThrow(NullPointerException::new);
    inner1 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("inner1"))
            .findFirst()
            .orElseThrow(NullPointerException::new);

    Assert.assertTrue(
        dep1.dependencies().stream()
            .peek(d -> System.out.println("dep1.dependencies(): " + d))
            .allMatch(d -> d.artifactId().equals("inner1") && d.version().equals("0.1")));
    Assert.assertTrue(
        dep2.dependencies().stream()
            .allMatch(d -> d.artifactId().equals("inner1") && d.version().equals("0.1")));
    Assert.assertEquals(0, inner1.dependencies().size());

    resolved = mUnderTest.mergeGraphs(Arrays.asList(root2, root3, root1));
    Assert.assertEquals(3, resolved.size());
    dep1 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("dep1"))
            .findFirst()
            .orElseThrow(NullPointerException::new);
    dep2 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("dep2"))
            .findFirst()
            .orElseThrow(NullPointerException::new);
    inner1 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("inner1"))
            .findFirst()
            .orElseThrow(NullPointerException::new);

    Assert.assertTrue(
        dep1.dependencies().stream()
            .peek(d -> System.out.println("dep1.dependencies(): " + d))
            .allMatch(d -> d.artifactId().equals("inner1") && d.version().equals("0.1")));
    Assert.assertTrue(
        dep2.dependencies().stream()
            .allMatch(d -> d.artifactId().equals("inner1") && d.version().equals("0.1")));
    Assert.assertEquals(0, inner1.dependencies().size());

    resolved = mUnderTest.mergeGraphs(Arrays.asList(root2, root1, root3));
    Assert.assertEquals(3, resolved.size());
    dep1 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("dep1"))
            .findFirst()
            .orElseThrow(NullPointerException::new);
    dep2 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("dep2"))
            .findFirst()
            .orElseThrow(NullPointerException::new);
    inner1 =
        resolved.stream()
            .filter(d -> d.mavenCoordinate().artifactId().equals("inner1"))
            .findFirst()
            .orElseThrow(NullPointerException::new);

    Assert.assertTrue(
        dep1.dependencies().stream()
            .peek(d -> System.out.println("dep1.dependencies(): " + d))
            .allMatch(d -> d.artifactId().equals("inner1") && d.version().equals("0.1")));
    Assert.assertTrue(
        dep2.dependencies().stream()
            .allMatch(d -> d.artifactId().equals("inner1") && d.version().equals("0.1")));
    Assert.assertEquals(0, inner1.dependencies().size());
  }
}
