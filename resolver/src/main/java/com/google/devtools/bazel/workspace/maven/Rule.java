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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.maven.model.License;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.JavaScopes;

import java.util.*;

/** A struct representing the fields of maven_jar to be written to the WORKSPACE file. */
// TODO(petros): Kill this after refactoring resolvers.
public class Rule implements Comparable<Rule> {

    private static final String DEFAULT_MAVEN_REPOSITORY = "https://repo1.maven.org/maven2/";

    private static final String DEFAULT_PACKAGING = "jar";

    private final Artifact artifact;
    private final Set<String> parents;
    private final Map<String, Set<Rule>> dependencies;
    private String version;
    private String repository;
    private String packaging;
    private String scope;
    private List<License> licenses = Collections.emptyList();

    public Rule(Artifact artifact) {
        this.artifact = artifact;
        this.version = artifact.getVersion();
        this.parents = Sets.newHashSet();
        this.dependencies = Maps.newHashMap();
        this.repository = DEFAULT_MAVEN_REPOSITORY;
        this.packaging = DEFAULT_PACKAGING;
    }

    static String generateFriendlyName(String groupId, String artifactId) {
        return normalizedWorkspaceName(groupId) + "__" + normalizedWorkspaceName(artifactId);
    }

    static String generateFullName(String groupId, String artifactId, String version) {
        return normalizedWorkspaceName(groupId)
                + "__"
                + normalizedWorkspaceName(artifactId)
                + "__"
                + normalizedWorkspaceName(version);
    }

    private static String normalizedWorkspaceName(final String name) {
        return name.replaceAll("[.-]", "_");
    }

    private static String normalizeMavenName(final String name) {
        return name.replace('.', '_');
    }

    public static String calculateMavenCoordinates(
            final String groupId, final String artifactId, final String version) {
        return groupId + ":" + artifactId + ":" + version;
    }

    public void addParent(String parent) {
        parents.add(parent);
    }

    public Set<Rule> getDeps() {
        return getDependencies(JavaScopes.COMPILE);
    }

    public Set<Rule> getExportDeps() {
        return getDependencies(JavaScopes.COMPILE);
    }

    public Set<Rule> getRuntimeDeps() {
        return getDependencies(JavaScopes.RUNTIME);
    }

    public void addDependency(String scope, Rule dependency) {
        if (!dependencies.containsKey(scope)) {
            dependencies.put(scope, Sets.newHashSet());
        }

        dependencies.get(scope).add(dependency);
    }

    public Set<Rule> getDependencies(String scope) {
        if (!dependencies.containsKey(scope)) {
            return Collections.emptySet();
        }
        return dependencies.get(scope);
    }

    public String artifactId() {
        return artifact.getArtifactId();
    }

    public String groupId() {
        return artifact.getGroupId();
    }

    public String version() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String packaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        if (MigrationToolingMavenResolver.isEmpty(packaging)) {
            return;
        }
        switch (packaging) {
            case "bundle":
            case "maven-plugin":
            case "eclipse-plugin":
                this.packaging = DEFAULT_PACKAGING;
                break;
            default:
                this.packaging = packaging;
                break;
        }
    }

    public String scope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * A unique name for this artifact which includes Maven meta-data. This can later be decoded
     * back into a maven annotation. Should be used as the name of a lib, never an external
     * workspace
     */
    public String mavenGeneratedName() {
        return normalizeMavenName(groupId())
                + "__"
                + normalizeMavenName(artifactId())
                + "__"
                + normalizeMavenName(version());
    }

    public String mavenCoordinates() {
        return calculateMavenCoordinates(groupId(), artifactId(), version());
    }

    public String mavenShortCoordinates() {
        return calculateMavenCoordinates(groupId(), artifactId(), "");
    }

    public Artifact getArtifact() {
        return artifact;
    }

    private String getUri() {
        return groupId().replaceAll("\\.", "/")
                + "/"
                + artifactId()
                + "/"
                + version()
                + "/"
                + artifactId()
                + "-"
                + version()
                + (artifact.getClassifier().isEmpty() ? "" : "-" + artifact.getClassifier())
                + "."
                + packaging();
    }

    public String getUrl() {
        Preconditions.checkState(
                repository.endsWith("/"), "Repository url '%s' should end with '/'", repository);
        return repository + getUri();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Rule rule = (Rule) o;

        return Objects.equals(groupId(), rule.groupId())
                && Objects.equals(artifactId(), rule.artifactId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId(), artifactId());
    }

    @Override
    public int compareTo(Rule o) {
        return mavenGeneratedName().compareTo(o.mavenGeneratedName());
    }

    public Set<String> getParents() {
        return parents;
    }

    public String classifier() {
        return artifact.getClassifier();
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(final String url) {
        repository = url;
    }

    public boolean isValid() {
        return !repository.isEmpty();
    }

    public Collection<License> getLicenses() {
        return this.licenses;
    }

    void setLicenses(final List<License> licenses) {
        this.licenses = licenses;
    }
}
