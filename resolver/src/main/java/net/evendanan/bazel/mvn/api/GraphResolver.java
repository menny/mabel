package net.evendanan.bazel.mvn.api;

import java.util.Collection;

public interface GraphResolver {
    Collection<Dependency> resolve(
            Collection<String> repositoriesUrls,
            Collection<String> mavenCoordinates,
            Collection<String> excludes);
}
