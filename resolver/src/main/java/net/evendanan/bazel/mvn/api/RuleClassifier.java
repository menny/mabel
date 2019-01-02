package net.evendanan.bazel.mvn.api;

import java.util.Optional;

public interface RuleClassifier {

    Optional<TargetsBuilder> classifyRule(Dependency dependency);

}
