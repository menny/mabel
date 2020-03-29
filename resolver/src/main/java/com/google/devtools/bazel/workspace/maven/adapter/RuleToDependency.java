package com.google.devtools.bazel.workspace.maven.adapter;

import com.google.devtools.bazel.workspace.maven.Rule;
import net.evendanan.bazel.mvn.api.LicenseTools;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class RuleToDependency {

    private final boolean debugLogs;

    public RuleToDependency(boolean debugLogs) {
        this.debugLogs = debugLogs;
    }

    private static MavenCoordinate ruleToMavenCoordinate(Rule rule) {
        return MavenCoordinate.create(
                rule.groupId(), rule.artifactId(), rule.version(), rule.packaging());
    }

    public Resolution from(Rule rule) {
        HashMap<String, Dependency> cache = new HashMap<>();
        Dependency rootDependency = from(rule, cache);

        return Resolution.create(rootDependency.mavenCoordinate(), cache.values());
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

        Dependency mapped =
                Dependency.builder()
                        .mavenCoordinate(ruleToMavenCoordinate(rule))
                        .dependencies(
                                rule.getDeps().stream()
                                        .map(RuleToDependency::ruleToMavenCoordinate)
                                        .collect(Collectors.toList()))
                        .exports(
                                rule.getExportDeps().stream()
                                        .map(RuleToDependency::ruleToMavenCoordinate)
                                        .collect(Collectors.toList()))
                        .runtimeDependencies(
                                rule.getRuntimeDeps().stream()
                                        .map(RuleToDependency::ruleToMavenCoordinate)
                                        .collect(Collectors.toList()))
                        .url(rule.isValid() ? rule.getUrl() : "")
                        .licenses(
                                rule.getLicenses().stream()
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
}
