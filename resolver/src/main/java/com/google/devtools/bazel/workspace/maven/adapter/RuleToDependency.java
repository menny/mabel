package com.google.devtools.bazel.workspace.maven.adapter;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.net.URI;
import java.util.Collections;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;

public class RuleToDependency {

    public static Dependency from(Rule rule) {
        return new Dependency(rule.groupId(), rule.artifactId(), rule.version(), rule.packaging(),
                rule.getDeps().stream().map(RuleToDependency::from).collect(Collectors.toList()),
                rule.getExportDeps().stream().map(RuleToDependency::from).collect(Collectors.toList()),
                rule.getRuntimeDeps().stream().map(RuleToDependency::from).collect(Collectors.toList()),
                URI.create(rule.getUrl()),
                URI.create(""),
                URI.create(""),
                Collections.emptyList());
    }
}
