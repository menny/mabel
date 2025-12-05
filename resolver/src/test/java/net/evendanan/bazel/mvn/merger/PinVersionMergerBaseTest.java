package net.evendanan.bazel.mvn.merger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;
import org.junit.Test;

public class PinVersionMergerBaseTest {

  private static class FakeMerger extends PinVersionMergerBase {
    @Override
    protected Dependency pickDependency(
        Dependency previousDependency, Dependency currentDependency) {
      // Always pick current to simulate replacement or simply non-null
      return currentDependency;
    }
  }

  @Test
  public void testMergeGraphs() {
    FakeMerger merger = new FakeMerger();

    MavenCoordinate rootCoord = MavenCoordinate.create("g", "root", "1.0", "jar");
    MavenCoordinate depCoord = MavenCoordinate.create("g", "dep", "1.0", "jar");

    Dependency dep = Dependency.builder().mavenCoordinate(depCoord).build();
    Dependency root =
        Dependency.builder()
            .mavenCoordinate(rootCoord)
            .dependencies(Collections.singleton(depCoord))
            .build();

    Resolution resolution = Resolution.create(rootCoord, Arrays.asList(root, dep));

    Collection<Dependency> result = merger.mergeGraphs(Collections.singletonList(resolution));

    // Should contain both dependencies
    assertEquals(2, result.size());
    assertTrue(result.stream().anyMatch(d -> d.mavenCoordinate().equals(rootCoord)));
    assertTrue(result.stream().anyMatch(d -> d.mavenCoordinate().equals(depCoord)));
  }

  @Test
  public void testDisconnectedGraph() {
    FakeMerger merger = new FakeMerger();

    MavenCoordinate c1 = MavenCoordinate.create("g", "1", "1.0", "jar");
    MavenCoordinate c2 = MavenCoordinate.create("g", "2", "1.0", "jar"); // No relation

    Dependency d1 = Dependency.builder().mavenCoordinate(c1).build();
    Dependency d2 = Dependency.builder().mavenCoordinate(c2).build();

    Resolution r1 = Resolution.create(c1, Collections.singletonList(d1));
    Resolution r2 = Resolution.create(c2, Collections.singletonList(d2));

    Collection<Dependency> result = merger.mergeGraphs(Arrays.asList(r1, r2));

    assertEquals(2, result.size());
  }

  @Test
  public void testCyclicGraph() {
    FakeMerger merger = new FakeMerger();

    MavenCoordinate c1 = MavenCoordinate.create("g", "1", "1.0", "jar");
    MavenCoordinate c2 = MavenCoordinate.create("g", "2", "1.0", "jar");

    // 1 depends on 2
    Dependency d1 =
        Dependency.builder().mavenCoordinate(c1).dependencies(Collections.singleton(c2)).build();
    // 2 depends on 1
    Dependency d2 =
        Dependency.builder().mavenCoordinate(c2).dependencies(Collections.singleton(c1)).build();

    Resolution r1 = Resolution.create(c1, Arrays.asList(d1, d2));

    Collection<Dependency> result = merger.mergeGraphs(Collections.singletonList(r1));

    assertEquals(2, result.size());
  }
}
