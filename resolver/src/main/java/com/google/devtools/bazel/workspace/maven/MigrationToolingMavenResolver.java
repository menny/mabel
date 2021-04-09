// Copyright 2015 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.bazel.workspace.maven;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.JavaScopes;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.google.devtools.bazel.workspace.maven.ArtifactBuilder.InvalidArtifactCoordinateException;

/**
 * Resolves Maven dependencies.
 */
public class MigrationToolingMavenResolver {

    /**
     * The set of scopes whose artifacts are pulled into the transitive dependency tree. See
     * https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html for
     * more details on how maven handles this.
     */
    private static final Set<String> INHERITED_SCOPES =
            Sets.newHashSet(JavaScopes.COMPILE, JavaScopes.RUNTIME);

    private static final Set<String> NON_INHERITED_SCOPES = Sets.newHashSet(JavaScopes.PROVIDED);
    private final boolean debugLogs;
    private final DefaultModelResolver modelResolver;
    private final Map<DepKey, Rule> deps;
    private final Map<String, String> restriction;
    private final Collection<Repository> repositories;
    private final VersionResolver versionResolver;
    private final Collection<String> blacklist;

    public MigrationToolingMavenResolver(
            Collection<Repository> repositories,
            DefaultModelResolver modelResolver,
            VersionResolver versionResolver,
            Collection<String> blacklist,
            boolean debugLogs) {
        this.repositories = repositories;
        this.versionResolver = versionResolver;
        this.deps = Maps.newHashMap();
        this.restriction = Maps.newHashMap();
        this.modelResolver = modelResolver;
        this.blacklist = blacklist;
        this.debugLogs = debugLogs;
    }

    private static String unversionedCoordinate(Dependency dependency) {
        return dependency.getGroupId() + ":" + dependency.getArtifactId();
    }

    private static String unversionedCoordinateGroup(Dependency dependency) {
        return dependency.getGroupId();
    }

    private static String unversionedCoordinate(Exclusion exclusion) {
        return exclusion.getGroupId() + ":" + exclusion.getArtifactId();
    }

    static boolean isEmpty(CharSequence text) {
        return text == null || text.length() == 0;
    }

    private static Rule createRuleForCoordinates(final String mavenCoordinate) {
        try {
            Artifact artifact = ArtifactBuilder.fromCoords(mavenCoordinate);
            return new Rule(artifact);
        } catch (ArtifactBuilder.InvalidArtifactCoordinateException e) {
            throw new IllegalArgumentException("Illegal Maven coordinates " + mavenCoordinate, e);
        }
    }

    /**
     * Resolves an artifact as a root of a dependency graph.
     */
    public Rule resolveRuleArtifacts(String mavenCoordinates) {
        final Rule rule = createRuleForCoordinates(mavenCoordinates);
        traverseRuleAndFill(rule, Sets.newHashSet(), Sets.newHashSet());
        return rule;
    }

    private void traverseRuleAndFill(final Rule rule, Set<String> scopes, Set<String> exclusions) {
        deps.put(DepKey.from(rule), rule);
        if (debugLogs) {
            System.out.println(
                    "Traversing "
                            + rule.mavenGeneratedName()
                            + ". Currently, have "
                            + deps.size()
                            + " resolved deps.");
        }
        DefaultModelResolver.RepoModelSource depModelSource;
        try {
            depModelSource =
                    modelResolver.resolveModel(
                            rule.groupId(),
                            rule.artifactId(),
                            rule.classifier() == null ? "" : rule.classifier(),
                            rule.version());
        } catch (UnresolvableModelException e) {
            depModelSource = null;
        }

        if (depModelSource != null) {
            Model depModel =
                    depModelSource.getModelSource() != null
                            ? modelResolver.getEffectiveModel(depModelSource.getModelSource())
                            : null;
            if (depModel != null) {
                rule.setPackaging(depModel.getPackaging());
                rule.setLicenses(depModel.getLicenses());
                rule.setRepository(depModelSource.getRepository().getUrl());
                traverseDeps(depModel, scopes, exclusions, rule);
            }
        } else {
            for (final Repository repository : repositories) {
                for (final String packaging : Arrays.asList("jar", "aar")) {
                    final URL urlForArtifact =
                            DefaultModelResolver.getUrlForArtifact(
                                    repository.getUrl(),
                                    rule.groupId(),
                                    rule.artifactId(),
                                    rule.classifier(),
                                    rule.version(),
                                    packaging);
                    if (DefaultModelResolver.remoteFileExists(urlForArtifact)) {
                        if (debugLogs)
                            System.out.println(
                                    "Could not get a model for "
                                            + rule.mavenGeneratedName()
                                            + ". Using direct artifact "
                                            + urlForArtifact);
                        rule.setPackaging(packaging);
                        rule.setLicenses(Collections.emptyList());
                        rule.setRepository(repository.getUrl());
                        return;
                    }
                }
            }

            rule.setRepository("");
            System.out.println(
                    "Could not get a model for "
                            + rule.mavenGeneratedName()
                            + ", and was unable to locate the artifact at any valid repository!");
        }
    }

    /**
     * Resolves all dependencies from a given "model source," which could be either a URL or a local
     * file.
     */
    private void traverseDeps(
            Model model, Set<String> scopes, Set<String> exclusions, Rule parent) {
        if (model.getDependencyManagement() != null) {
            // Dependencies described in the DependencyManagement section of the pom override all
            // others,
            // so resolve them first.
            for (Dependency dependency : model.getDependencyManagement().getDependencies()) {
                restriction.put(
                        Rule.generateFriendlyName(
                                dependency.getGroupId(), dependency.getArtifactId()),
                        dependency.getVersion());
            }
        }
        for (Dependency dependency : model.getDependencies()) {
            if (debugLogs) {
                System.out.println("Found dependency " + dependency);
            }
            addDependency(dependency, model, scopes, exclusions, parent);
        }
    }

    private void addDependency(
            Dependency dependency,
            Model model,
            Set<String> topLevelScopes,
            Set<String> exclusions,
            Rule parent) {
        String scope = isEmpty(dependency.getScope()) ? JavaScopes.COMPILE : dependency.getScope();
        // TODO (bazel-devel): Relabel the scope of transitive dependencies so that they match how
        // maven relabels them as described here:
        // https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html
        if (!INHERITED_SCOPES.contains(scope) && !NON_INHERITED_SCOPES.contains(scope)) {
            return;
        }

        if (dependency.isOptional()) {
            return;
        }
        if (exclusions.contains(unversionedCoordinate(dependency))
                || exclusions.contains(unversionedCoordinateGroup(dependency))) {
            return;
        }

        if (blacklist.contains(unversionedCoordinate(dependency))
                || blacklist.contains(unversionedCoordinateGroup(dependency))) {
            return;
        }

        Rule artifactRule;
        try {
            artifactRule =
                    new Rule(
                            ArtifactBuilder.fromMavenDependency(
                                    dependency, versionResolver, model));
        } catch (InvalidArtifactCoordinateException e) {
            throw new RuntimeException(
                    String.format(Locale.ROOT, "Dependency '%s' has invalid format!", dependency),
                    e);
        }
        artifactRule.setScope(scope);

        HashSet<String> localDepExclusions = Sets.newHashSet(exclusions);
        dependency
                .getExclusions()
                .forEach(exclusion -> localDepExclusions.add(unversionedCoordinate(exclusion)));

        final DepKey depKey = DepKey.from(artifactRule);
        if (deps.containsKey(depKey)) {
            // already traverse this one
            artifactRule = deps.get(depKey);
        } else {
            traverseRuleAndFill(artifactRule, topLevelScopes, localDepExclusions);
        }

        // for comments
        artifactRule.addParent(parent.mavenCoordinates());

        // for graph
        parent.addDependency(artifactRule.scope(), artifactRule);
    }

    private static class DepKey {
        private final Rule rule;
        private final String key;

        private DepKey(final Rule rule) {
            this.rule = rule;
            key = rule.mavenCoordinates();
        }

        private static DepKey from(final Rule artifactRule) {
            return new DepKey(artifactRule);
        }

        String key() {
            return key;
        }

        @Override
        public int hashCode() {
            return key().hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof DepKey) {
                return ((DepKey) obj).key().equals(key());
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "DepKey " + key() + " for " + rule.mavenGeneratedName();
        }
    }
}
