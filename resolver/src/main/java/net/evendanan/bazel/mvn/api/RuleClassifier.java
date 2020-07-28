package net.evendanan.bazel.mvn.api;

import net.evendanan.bazel.mvn.api.model.Dependency;

import java.util.List;

public interface RuleClassifier {

    List<TargetsBuilder> classifyRule(Dependency dependency);
}
