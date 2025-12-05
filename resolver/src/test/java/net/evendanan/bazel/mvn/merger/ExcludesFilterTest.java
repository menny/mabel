package net.evendanan.bazel.mvn.merger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import org.junit.Test;

public class ExcludesFilterTest {

  private final MavenCoordinate dep1 =
      MavenCoordinate.create("com.google.guava", "guava", "1.0", "jar");
  private final MavenCoordinate dep2 =
      MavenCoordinate.create("org.apache", "commons", "1.0", "jar");
  private final MavenCoordinate child =
      MavenCoordinate.create("com.google.code", "findbugs", "1.0", "jar");

  @Test
  public void testSimpleExclusion() {
    Dependency d1 = Dependency.builder().mavenCoordinate(dep1).build();
    Dependency d2 = Dependency.builder().mavenCoordinate(dep2).build();

    Collection<Dependency> result =
        ExcludesFilter.filterDependencies(
            Arrays.asList(d1, d2), Collections.singleton("com.google"));

    assertEquals(1, result.size());
    assertEquals("org.apache", result.iterator().next().mavenCoordinate().groupId());
  }

  @Test
  public void testTransitiveExclusion() {
    // Parent implies child
    Dependency parent =
        Dependency.builder()
            .mavenCoordinate(dep2)
            .dependencies(Arrays.asList(dep1, child)) // dep1 is excluded, child matches excluded
            .build();

    Collection<Dependency> result =
        ExcludesFilter.filterDependencies(
            Collections.singletonList(parent), Collections.singleton("com.google"));

    assertEquals(1, result.size());
    Dependency res = result.iterator().next();
    assertEquals(0, res.dependencies().size());
    // Both com.google.guava and com.google.code should be filtered out because they
    // start with "com.google"
  }

  @Test
  public void testEmptyExcludesString() {
    // Edge case: if exclude string is empty "", inputs.startsWith("") is true for
    // all strings.
    // So everything should be excluded.
    Dependency d1 = Dependency.builder().mavenCoordinate(dep1).build();

    Collection<Dependency> result =
        ExcludesFilter.filterDependencies(Collections.singletonList(d1), Collections.singleton(""));

    assertTrue(result.isEmpty());
  }

  @Test
  public void testStrictPrefix() {
    // "com.google" matches "com.google.guava"
    // "com.googler" does NOT match "com.google.guava"
    // but inputs check: dependency.startsWith(exclude)
    // so "com.google.guava".startsWith("com.google") is TRUE
    // "com.google.guava".startsWith("com.googler") is FALSE (Correct)
    // AND "com.googlers".startsWith("com.google") is TRUE.

    Dependency d1 =
        Dependency.builder()
            .mavenCoordinate(MavenCoordinate.create("com.googlers", "something", "1.0", "jar"))
            .build();

    Collection<Dependency> result =
        ExcludesFilter.filterDependencies(
            Collections.singletonList(d1), Collections.singleton("com.google"));

    // This confirms the behavioral logic: string prefix matching.
    assertTrue(result.isEmpty());
  }
}
