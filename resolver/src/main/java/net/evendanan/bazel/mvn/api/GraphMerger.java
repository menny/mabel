package net.evendanan.bazel.mvn.api;

import java.util.Collection;

public interface GraphMerger {
    Collection<Dependency> mergeGraphs(
            Collection<Dependency> dependencies,
            Collection<String> excludes);
}
