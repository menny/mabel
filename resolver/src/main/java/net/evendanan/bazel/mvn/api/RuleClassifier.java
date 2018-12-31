package net.evendanan.bazel.mvn.api;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.util.Optional;

public interface RuleClassifier {

    Optional<TargetsBuilder> classifyRule(Rule rule);

}
