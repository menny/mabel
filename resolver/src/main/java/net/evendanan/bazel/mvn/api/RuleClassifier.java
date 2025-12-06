package net.evendanan.bazel.mvn.api;

import java.util.List;
import net.evendanan.bazel.mvn.api.model.Dependency;

public interface RuleClassifier {

  List<TargetsBuilder> classifyRule(Dependency dependency);
}
