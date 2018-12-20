package net.evendanan.bazel.mvn;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.util.Optional;

public interface RuleClassifier {

    Optional<RuleFormatter> classifyRule(Rule rule);

}
