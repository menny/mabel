package com.google.devtools.bazel.workspace.maven.adapter;

import com.google.devtools.bazel.workspace.maven.Rule;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.LicenseTools;
import net.evendanan.bazel.mvn.api.MavenCoordinate;
import net.evendanan.bazel.mvn.api.Resolution;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class RuleToDependency {

    private final boolean debugLogs;

    public RuleToDependency(boolean debugLogs) {
        this.debugLogs = debugLogs;
    }

    public Resolution from(Rule rule) {
        HashMap<String, Dependency> cache = new HashMap<>();
        Dependency rootDependency = from(rule, cache);

        return Resolution.newBuilder()
                .setRootDependency(rootDependency.getMavenCoordinate())
                .addAllAllResolvedDependencies(cache.values())
                .build();
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
                .setMavenCoordinate(ruleToMavenCoordinate(rule))
                .addAllDependencies(rule.getDeps().stream().map(RuleToDependency::ruleToMavenCoordinate).collect(Collectors.toList()))
                .addAllExports(rule.getExportDeps().stream().map(RuleToDependency::ruleToMavenCoordinate).collect(Collectors.toList()))
                .addAllRuntimeDependencies(rule.getRuntimeDeps().stream().map(RuleToDependency::ruleToMavenCoordinate).collect(Collectors.toList()))
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

        rule.getDeps().forEach(dep -> cache.put(dep.mavenCoordinates(), from(dep, cache)));
        rule.getExportDeps().forEach(dep -> cache.put(dep.mavenCoordinates(), from(dep, cache)));
        rule.getRuntimeDeps().forEach(dep -> cache.put(dep.mavenCoordinates(), from(dep, cache)));

        return mapped;
    }

    private static MavenCoordinate ruleToMavenCoordinate(Rule rule) {
        return MavenCoordinate.newBuilder()
                .setGroupId(rule.groupId())
                .setArtifactId(rule.artifactId())
                .setVersion(rule.version())
                .setPackaging(rule.packaging())
                .build();
    }
}
