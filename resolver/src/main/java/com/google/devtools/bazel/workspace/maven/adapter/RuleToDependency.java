package com.google.devtools.bazel.workspace.maven.adapter;

import com.google.devtools.bazel.workspace.maven.Rule;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.LicenseTools;

public class RuleToDependency {

    private final boolean debugLogs;

    public RuleToDependency(boolean debugLogs) {
        this.debugLogs = debugLogs;
    }

    public Dependency from(Rule rule) {
        return from(rule, new HashMap<>());
    }

    public Dependency from(Rule rule, HashMap<String, Dependency> cache) {
        if (debugLogs) {
            System.out.println("Resolving dependencies for " + rule.mavenCoordinates());
        }
        if (cache.containsKey(rule.mavenCoordinates())) {
            if (debugLogs) {
                System.out.println("Dependencies taken from cache for " + rule.mavenCoordinates());
            }
            return cache.get(rule.mavenCoordinates());
        }

        Dependency mapped = Dependency.newBuilder()
                .setGroupId(rule.groupId())
                .setArtifactId(rule.artifactId())
                .setVersion(rule.version())
                .setPackaging(rule.packaging())
                .addAllDependencies(rule.getDeps().stream().map(dep -> from(dep, cache)).collect(Collectors.toList()))
                .addAllExports(rule.getExportDeps().stream().map(dep -> from(dep, cache)).collect(Collectors.toList()))
                .addAllRuntimeDependencies(rule.getRuntimeDeps().stream().map(dep -> from(dep, cache)).collect(Collectors.toList()))
                .setUrl(rule.isValid() ? rule.getUrl() : "")
                .addAllLicenses(rule.getLicenses().stream()
                        .map(org.apache.maven.model.License::getName)
                        .map(LicenseTools::fromLicenseName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .build();

        if (debugLogs) {
            System.out.println("Caching dependencies for " + rule.mavenCoordinates());
        }
        cache.put(rule.mavenCoordinates(), mapped);
        return mapped;
    }
}
