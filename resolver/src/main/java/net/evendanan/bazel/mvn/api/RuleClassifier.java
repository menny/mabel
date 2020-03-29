package net.evendanan.bazel.mvn.api;

import net.evendanan.bazel.mvn.api.model.Dependency;

import java.util.Optional;

public interface RuleClassifier {

    Optional<TargetsBuilder> classifyRule(Dependency dependency);
}
