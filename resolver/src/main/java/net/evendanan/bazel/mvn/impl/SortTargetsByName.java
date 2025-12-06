package net.evendanan.bazel.mvn.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import net.evendanan.bazel.mvn.api.Target;

class SortTargetsByName {

  private static final Comparator<? super Target> TARGET_COMPARATOR =
      Comparator.comparing(Target::getMavenCoordinates).thenComparing(Target::getTargetName);

  public static Collection<Target> sort(Collection<Target> targets) {
    ArrayList<Target> sorted = new ArrayList<>(targets);
    sorted.sort(TARGET_COMPARATOR);

    return sorted;
  }
}
