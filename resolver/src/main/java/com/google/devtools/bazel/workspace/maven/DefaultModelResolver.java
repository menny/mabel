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

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.*;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.apache.maven.model.building.ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL;

/**
 * MigrationToolingMavenResolver to find the repository a given Maven artifact should be fetched
 * from.
 */
public class DefaultModelResolver implements ModelResolver {

    private final Collection<Repository> repositories;
    private final Map<String, RepoModelSource> ruleNameToModelSource;
    private final DefaultModelBuilder modelBuilder;
    private final VersionResolver versionResolver;

    public DefaultModelResolver(
            Collection<Repository> repositories, VersionResolver versionResolver) {
        this(
                repositories,
                Maps.newHashMap(),
                new DefaultModelBuilderFactory()
                        .newInstance()
                        .setProfileSelector(new DefaultProfileSelector()),
                versionResolver);
    }

    private DefaultModelResolver(
            Collection<Repository> repositories,
            Map<String, RepoModelSource> ruleNameToModelSource,
            DefaultModelBuilder modelBuilder,
            VersionResolver versionResolver) {
        this.repositories = repositories;
        this.ruleNameToModelSource = ruleNameToModelSource;
        this.modelBuilder = modelBuilder;
        this.versionResolver = versionResolver;
    }

    static boolean remoteFileExists(URL url) {
        try {
            URLConnection urlConnection = url.openConnection();
            if (!(urlConnection instanceof HttpURLConnection)) {
                return false;
            }

            HttpURLConnection connection = (HttpURLConnection) urlConnection;
            connection.setRequestMethod("HEAD");
            connection.setInstanceFollowRedirects(true);
            connection.connect();

            int code = connection.getResponseCode();
            return code == 200;
        } catch (IOException e) {
            return false;
        }
    }

    static URL getUrlForArtifact(
            String url,
            final String groupId,
            final String artifactId,
            final String classifier,
            final String version,
            final String packaging) {
        if (!url.endsWith("/")) {
            url += "/";
        }

        try {
            return new URL(
                    url
                            + groupId.replaceAll("\\.", "/")
                            + "/"
                            + artifactId
                            + "/"
                            + version
                            + "/"
                            + artifactId
                            + "-"
                            + version
                            + "."
                            + packaging);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        return resolveModel(groupId, artifactId, "", version).modelSource;
    }

    public RepoModelSource resolveModel(
            String groupId, String artifactId, String classifier, String version)
            throws UnresolvableModelException {
        String ruleName = Rule.generateFullName(groupId, artifactId, version);
        if (ruleNameToModelSource.containsKey(ruleName)) {
            return ruleNameToModelSource.get(ruleName);
        }

        for (Repository repository : repositories) {
            UrlModelSource modelSource =
                    getModelSource(repository.getUrl(), groupId, artifactId, classifier, version);
            if (modelSource != null) {
                final RepoModelSource repoModelSource =
                        new RepoModelSource(modelSource, repository);
                ruleNameToModelSource.put(
                        Rule.generateFullName(groupId, artifactId, version), repoModelSource);
                return repoModelSource;
            }
        }

        List<String> attemptedUrls =
                repositories.stream().map(Repository::getUrl).collect(toList());
        throw new UnresolvableModelException(
                "Could not find any repositories that knew how to "
                        + "resolve "
                        + groupId
                        + ":"
                        + artifactId
                        + ":"
                        + version
                        + " (checked "
                        + Joiner.on(", ").join(attemptedUrls)
                        + ")",
                groupId,
                artifactId,
                version);
    }

    // TODO(kchodorow): make this work with local repositories.
    private UrlModelSource getModelSource(
            String url, String groupId, String artifactId, String classifier, String version) {
        try {
            version = versionResolver.resolveVersion(groupId, artifactId, classifier, version);
        } catch (ArtifactBuilder.InvalidArtifactCoordinateException e) {
            throw new RuntimeException(
                    String.format(
                            "Unable to resolve version %s:%s:%s! %s",
                            groupId, artifactId, version, e.getMessage()),
                    e);
        }
        URL pomUrl = getUrlForArtifact(url, groupId, artifactId, classifier, version, "pom");
        if (remoteFileExists(pomUrl)) {
            return new UrlModelSource(pomUrl);
        }
        return null;
    }

    // For compatibility with older versions of ModelResolver which don't have this method,
    @Override
    public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), "", parent.getVersion())
                .modelSource;
    }

    @Override
    public void addRepository(Repository repository) {
        addRepository(repository, true);
    }

    @Override
    public void addRepository(Repository repository, boolean replace) {
        if (replace) {
            repositories.remove(repository);
        }

        repositories.add(repository);
    }

    @Override
    public ModelResolver newCopy() {
        return new DefaultModelResolver(
                repositories, ruleNameToModelSource, modelBuilder, versionResolver);
    }

    public Model getEffectiveModel(ModelSource modelSource) {
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelResolver(this);
        request.setValidationLevel(VALIDATION_LEVEL_MINIMAL);
        request.setModelSource(modelSource);
        Model model;

        ModelBuildingResult result;
        try {
            result = modelBuilder.build(request);
        } catch (ModelBuildingException e) {
            // IllegalArg can be thrown if the parent POM cannot be resolved.
            System.out.println(
                    "Unable to build Maven model from "
                            + modelSource.getLocation()
                            + ": "
                            + e.getMessage());
            return null;
        }

        try {
            model = result.getEffectiveModel();
        } catch (Exception e) {
            System.out.println(
                    "Unable to resolve effective Maven model from "
                            + modelSource.getLocation()
                            + ": "
                            + e.getMessage());
            return null;
        }

        return model;
    }

    public static class RepoModelSource {
        private final ModelSource modelSource;
        private final Repository repository;

        private RepoModelSource(final ModelSource modelSource, final Repository repository) {
            this.modelSource = modelSource;
            this.repository = repository;
        }

        public ModelSource getModelSource() {
            return modelSource;
        }

        public Repository getRepository() {
            return repository;
        }
    }
}
