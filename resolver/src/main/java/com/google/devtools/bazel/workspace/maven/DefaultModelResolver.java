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

import static java.util.stream.Collectors.toList;
import static org.apache.maven.model.building.ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * GraphResolver to find the repository a given Maven artifact should be fetched
 * from.
 */
public class DefaultModelResolver implements ModelResolver {

    private final static Logger logger = Logger.getLogger(
        MethodHandles.lookup().lookupClass().getName());

    private final Collection<Repository> repositories;
    private final Map<String, RepoModelSource> ruleNameToModelSource;
    private final DefaultModelBuilder modelBuilder;
    private final VersionResolver versionResolver;

    public DefaultModelResolver(Collection<Repository> repositories) {
        this(
            repositories,
            Maps.newHashMap(),
            new DefaultModelBuilderFactory().newInstance()
                .setProfileSelector(new DefaultProfileSelector())
        );
    }

    private DefaultModelResolver(Collection<Repository> repositories, Map<String, RepoModelSource> ruleNameToModelSource,
                                 DefaultModelBuilder modelBuilder) {
        this.repositories = repositories;
        this.ruleNameToModelSource = ruleNameToModelSource;
        this.modelBuilder = modelBuilder;
        final Aether aether = Aether.builder()
            .remoteRepos(repositories.stream().map(repo -> new RemoteRepository.Builder(repo.getId(), null, repo.getUrl()).build())
                .collect(toList()))
            .build();
        this.versionResolver = new VersionResolver(aether);
    }

    public RepoModelSource resolveModel(Artifact artifact) throws UnresolvableModelException {
        return resolveModel(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getVersion());
    }

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version)
        throws UnresolvableModelException {
        return resolveModel(groupId, artifactId, "", version).modelSource;
    }

    public RepoModelSource resolveModel(String groupId, String artifactId, String classifier, String version)
        throws UnresolvableModelException {
        String ruleName = Rule.generateFullName(groupId, artifactId, version);
        if (ruleNameToModelSource.containsKey(ruleName)) {
            return ruleNameToModelSource.get(ruleName);
        }
        for (Repository repository : repositories) {
            UrlModelSource modelSource = getModelSource(
                repository.getUrl(), groupId, artifactId, classifier, version);
            if (modelSource != null) {
                final RepoModelSource repoModelSource = new RepoModelSource(modelSource, repository);
                ruleNameToModelSource.put(Rule.generateFullName(groupId, artifactId, version), repoModelSource);
                return repoModelSource;
            }
        }

        List<String> attemptedUrls =
            repositories.stream().map(Repository::getUrl).collect(toList());
        throw new UnresolvableModelException("Could not find any repositories that knew how to "
                                             + "resolve " + groupId + ":" + artifactId + ":" + version + " (checked "
                                             + Joiner.on(", ").join(attemptedUrls) + ")", groupId, artifactId, version);
    }

    // TODO(kchodorow): make this work with local repositories.
    private UrlModelSource getModelSource(
        String url, String groupId, String artifactId, String classifier, String version)
        throws UnresolvableModelException {
        try {
            version = versionResolver.resolveVersion(groupId, artifactId, classifier, version);
        } catch (ArtifactBuilder.InvalidArtifactCoordinateException e) {
            logger.warning(String.format("Unable to resolve version %s:%s:%s! %s", groupId, artifactId, version, e.getMessage()));
        }
        try {
            if (!url.endsWith("/")) {
                url += "/";
            }
            URL urlUrl = new URL(url
                                 + groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version + "/" + artifactId
                                 + "-" + version + ".pom");
            if (pomFileExists(urlUrl)) {
                return new UrlModelSource(urlUrl);
            }
        } catch (MalformedURLException e) {
            throw new UnresolvableModelException("Bad URL " + url + ": " + e.getMessage(), groupId,
                artifactId, version, e);
        }
        return null;
    }

    private boolean pomFileExists(URL url) {
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
            if (code == 200) {
                return true;
            }
        } catch (IOException e) {
            // Something went wrong, fall through.
        }
        return false;
    }

    // For compatibility with older versions of ModelResolver which don't have this method,
    @Override
    public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), "", parent.getVersion()).modelSource;
    }

    // For compatibility with older versions of ModelResolver which don't have this method,
    // don't add @Override.
    public void addRepository(Repository repository) {
        repositories.add(repository);
    }

    @Override
    public void addRepository(Repository repository, boolean replace) {
        addRepository(repository);
    }

    @Override
    public ModelResolver newCopy() {
        return new DefaultModelResolver(repositories, ruleNameToModelSource, modelBuilder);
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
            logger.warning("Unable to build Maven model from " + modelSource.getLocation()
                           + ": " + e.getMessage());
            return null;
        }

        try {
            model = result.getEffectiveModel();
        } catch (Exception e) {
            logger.warning("Unable to resolve effective Maven model from " + modelSource.getLocation()
                           + ": " + e.getMessage());
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
