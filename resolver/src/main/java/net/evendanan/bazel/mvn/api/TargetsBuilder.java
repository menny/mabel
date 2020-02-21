package net.evendanan.bazel.mvn.api;

import java.util.List;
import net.evendanan.bazel.mvn.api.model.Dependency;

public interface TargetsBuilder {

    List<Target> buildTargets(Dependency dependency, DependencyTools dependencyTools);
}
