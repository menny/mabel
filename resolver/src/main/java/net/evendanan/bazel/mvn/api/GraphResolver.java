package net.evendanan.bazel.mvn.api;

import net.evendanan.bazel.mvn.api.model.Resolution;

import java.util.Collection;

public interface GraphResolver {
    Resolution resolve(
            String mavenCoordinate,
            Collection<String> repositoriesUrls,
            Collection<String> excludes);
}
