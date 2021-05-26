package net.evendanan.bazel.mvn.api;

import net.evendanan.bazel.mvn.api.model.Resolution;
import net.evendanan.bazel.mvn.api.model.TargetType;

import java.util.Collection;

public interface GraphResolver {
    Resolution resolve(
            TargetType type,
            String mavenCoordinate,
            Collection<String> repositoriesUrls,
            Collection<String> excludes);
}
