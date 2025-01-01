package net.evendanan.bazel.mvn.api;

import java.util.Collection;
import net.evendanan.bazel.mvn.api.model.Resolution;

public interface GraphResolver {
  Resolution resolve(
      String mavenCoordinate, Collection<String> repositoriesUrls, Collection<String> excludes);
}
