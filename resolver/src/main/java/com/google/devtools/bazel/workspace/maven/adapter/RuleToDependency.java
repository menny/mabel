package com.google.devtools.bazel.workspace.maven.adapter;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.net.URI;
import java.util.Objects;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;
import org.apache.maven.model.License;

public class RuleToDependency {

    public static Dependency from(Rule rule) {
        return new Dependency(rule.groupId(), rule.artifactId(), rule.version(), rule.packaging(),
                rule.getDeps().stream().map(RuleToDependency::from).collect(Collectors.toList()),
                rule.getExportDeps().stream().map(RuleToDependency::from).collect(Collectors.toList()),
                rule.getRuntimeDeps().stream().map(RuleToDependency::from).collect(Collectors.toList()),
                rule.isValid() ? URI.create(rule.getUrl()) : URI.create(""),
                URI.create(""),
                URI.create(""),
                rule.getLicenses().stream()
                        .map(License::getName)
                        .map(net.evendanan.bazel.mvn.api.Dependency.License::fromLicenseName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }
}
