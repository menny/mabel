package net.evendanan.bazel.mvn.merger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.Resolution;

public class PinLatestVersionMerger extends PinVersionMergerBase {
  private final VersionComparator mVersionStringComparator = new VersionComparator();
  private final Map<String, Dependency> mRootPinnedVersions = new HashMap<>();

  @Override
  protected Dependency pickDependency(Dependency previousDependency, Dependency currentDependency) {
    final String key = dependencyKey(currentDependency.mavenCoordinate());
    if (mRootPinnedVersions.containsKey(key)) return mRootPinnedVersions.get(key);

    if (previousDependency == null || previousDependency.url().isEmpty()) return currentDependency;

    final int diff =
        mVersionStringComparator.compare(
            previousDependency.mavenCoordinate().version(),
            currentDependency.mavenCoordinate().version());
    if (diff < 0) return currentDependency;
    else return previousDependency;
  }

  @Override
  public Collection<Dependency> mergeGraphs(final Collection<Resolution> resolutions) {
    mRootPinnedVersions.clear();
    resolutions.forEach(
        r ->
            mRootPinnedVersions.put(
                dependencyKey(r.rootDependency()),
                r.allResolvedDependencies().stream()
                    .filter(d -> d.mavenCoordinate().equals(r.rootDependency()))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new)));

    return super.mergeGraphs(resolutions);
  }
}
