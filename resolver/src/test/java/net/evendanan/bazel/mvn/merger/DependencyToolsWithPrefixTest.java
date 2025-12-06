package net.evendanan.bazel.mvn.merger;

import static org.junit.Assert.assertEquals;

import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import org.junit.Test;

public class DependencyToolsWithPrefixTest {

  @Test
  public void testTargetNamePrefix() {
    DependencyToolsWithPrefix tools = new DependencyToolsWithPrefix("prefix_");
    MavenCoordinate coord = MavenCoordinate.create("group", "artifact", "1.0.0", "jar");

    // Default DependencyTools uses "group__artifact" style (roughly)
    String name = tools.targetName(coord);
    assertEquals("prefix_group__artifact", name);
  }

  @Test
  public void testRepositoryNamePrefix() {
    DependencyToolsWithPrefix tools = new DependencyToolsWithPrefix("prefix_");
    MavenCoordinate coord = MavenCoordinate.create("group", "artifact", "1.0.0", "jar");

    String name = tools.repositoryRuleName(coord);
    assertEquals("prefix_group__artifact__1_0_0", name);
  }

  @Test
  public void testEmptyPrefix() {
    DependencyToolsWithPrefix tools = new DependencyToolsWithPrefix("");
    MavenCoordinate coord = MavenCoordinate.create("group", "artifact", "1.0.0", "jar");

    assertEquals("group__artifact", tools.targetName(coord));
  }
}
