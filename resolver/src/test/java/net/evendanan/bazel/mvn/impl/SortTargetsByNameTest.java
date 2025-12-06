package net.evendanan.bazel.mvn.impl;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import net.evendanan.bazel.mvn.api.Target;
import org.junit.Test;

public class SortTargetsByNameTest {

  @Test
  public void testSortByCoordinate() {
    Target t1 = new Target("group:artifact:1.0.0", "rule", "name1", "");
    Target t2 = new Target("group:artifact:2.0.0", "rule", "name2", "");
    Target t3 = new Target("another:artifact:1.0.0", "rule", "name3", "");

    Collection<Target> sorted = SortTargetsByName.sort(Arrays.asList(t2, t1, t3));
    Iterator<Target> iterator = sorted.iterator();

    assertEquals(t3, iterator.next()); // another...
    assertEquals(t1, iterator.next()); // group:artifact:1.0.0
    assertEquals(t2, iterator.next()); // group:artifact:2.0.0
  }

  @Test
  public void testSortByNameWhenCoordinatesIdentical() {
    Target t1 = new Target("group:artifact:1.0.0", "rule", "zzz", "");
    Target t2 = new Target("group:artifact:1.0.0", "rule", "aaa", "");

    Collection<Target> sorted = SortTargetsByName.sort(Arrays.asList(t1, t2));
    Iterator<Target> iterator = sorted.iterator();

    assertEquals(t2, iterator.next()); // aaa
    assertEquals(t1, iterator.next()); // zzz
  }

  @Test
  public void testEmptyList() {
    Collection<Target> sorted = SortTargetsByName.sort(Collections.emptyList());
    assertEquals(0, sorted.size());
  }

  @Test
  public void testSingletonList() {
    Target t1 = new Target("group:artifact:1.0.0", "rule", "name1", "");
    Collection<Target> sorted = SortTargetsByName.sort(Collections.singletonList(t1));
    assertEquals(1, sorted.size());
    assertEquals(t1, sorted.iterator().next());
  }

  @Test
  public void testStability() {
    Target t1 = new Target("group:artifact:1.0.0", "rule", "name1", "");
    Target t2 = new Target("group:artifact:1.0.0", "rule", "name1", "");

    // Since they are equal in comparator, original order should ideally be
    // preserved or arbitrary but consistent.
    // The implementation uses ArrayList.sort which is stable.
    Collection<Target> sorted = SortTargetsByName.sort(Arrays.asList(t1, t2));
    Iterator<Target> iterator = sorted.iterator();

    assertEquals(t1, iterator.next());
    assertEquals(t2, iterator.next());
  }
}
