package net.evendanan.bazel.mvn.api;

import java.util.Optional;
import net.evendanan.bazel.mvn.api.model.Dependency;

public interface RuleClassifier {

    Optional<TargetsBuilder> classifyRule(Dependency dependency);

}
