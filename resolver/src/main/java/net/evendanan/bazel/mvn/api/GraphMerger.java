package net.evendanan.bazel.mvn.api;

import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.Resolution;

import java.util.Collection;

public interface GraphMerger {
    Collection<Dependency> mergeGraphs(Collection<Resolution> dependencies);
}
