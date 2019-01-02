package net.evendanan.bazel.mvn.api;

import java.util.List;

public interface TargetsBuilder {

    List<Target> buildTargets(Dependency dependency);
}
