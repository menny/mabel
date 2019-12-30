package net.evendanan.bazel.mvn.api;

import java.util.Collection;

public interface GraphResolver {
    Resolution resolve(
            String mavenCoordinate,
            Collection<String> repositoriesUrls,
            Collection<String> excludes);
}
