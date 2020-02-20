package net.evendanan.bazel.mvn.api;

import java.util.Collection;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.Resolution;

public interface GraphMerger {
    Collection<Dependency> mergeGraphs(
            Collection<Resolution> dependencies);
}
