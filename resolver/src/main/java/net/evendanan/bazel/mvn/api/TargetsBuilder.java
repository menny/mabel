package net.evendanan.bazel.mvn.api;

import net.evendanan.bazel.mvn.api.model.Dependency;

import java.util.List;

public interface TargetsBuilder {

    List<Target> buildTargets(Dependency dependency, DependencyTools dependencyTools);
}
