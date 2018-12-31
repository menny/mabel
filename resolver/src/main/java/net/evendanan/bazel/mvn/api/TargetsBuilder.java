package net.evendanan.bazel.mvn.api;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.util.List;

public interface TargetsBuilder {

    List<Target> buildTargets(Rule rule);
}
