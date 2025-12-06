package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.model.Dependency;

public class PinBreadthFirstVersionsMerger extends PinVersionMergerBase {

  @Override
  protected Dependency pickDependency(Dependency previousDependency, Dependency currentDependency) {
    if (previousDependency == null || previousDependency.url().isEmpty()) return currentDependency;
    else return previousDependency;
  }
}
