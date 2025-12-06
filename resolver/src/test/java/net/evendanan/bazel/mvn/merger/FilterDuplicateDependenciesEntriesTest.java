package net.evendanan.bazel.mvn.merger;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import org.junit.Test;

public class FilterDuplicateDependenciesEntriesTest {

  @Test
  public void testDeduplication() {
    MavenCoordinate coord = MavenCoordinate.create("g", "a", "1.0", "jar");
    Dependency d1 = Dependency.builder().mavenCoordinate(coord).url("url1").build();
    Dependency d2 =
        Dependency.builder().mavenCoordinate(coord).url("url2").build(); // Duplicate coordinate,
    // different URL/obj

    Collection<Dependency> result =
        FilterDuplicateDependenciesEntries.filterDuplicateDependencies(Arrays.asList(d1, d2));

    // Should likely keep the first one encountered or unique by coordinate
    assertEquals(1, result.size());
    assertEquals("url1", result.iterator().next().url());
  }

  @Test
  public void testDeduplicateInnerDependencies() {
    MavenCoordinate depCoord = MavenCoordinate.create("g", "b", "1.0", "jar");

    Dependency root =
        Dependency.builder()
            .mavenCoordinate(MavenCoordinate.create("g", "root", "1.0", "jar"))
            .dependencies(Arrays.asList(depCoord, depCoord)) // Duplicate in list
            .build();

    Collection<Dependency> result =
        FilterDuplicateDependenciesEntries.filterDuplicateDependencies(Arrays.asList(root));

    Dependency res = result.iterator().next();
    assertEquals(1, res.dependencies().size());
  }
}
