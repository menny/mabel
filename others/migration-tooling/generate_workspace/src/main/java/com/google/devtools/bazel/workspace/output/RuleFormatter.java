package com.google.devtools.bazel.workspace.output;

import com.google.devtools.bazel.workspace.maven.Rule;

public interface RuleFormatter {

    String formatRule(Rule rule);
}
