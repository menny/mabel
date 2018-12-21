// Copyright 2017 The Bazel Authors. All rights reserved.
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

import com.google.common.collect.Lists;
import java.util.List;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.version.Version;

/**
 * Facade around aether. This class is used to make various requests to the aether subsystem.
 */
public class Aether {

    private RepositorySystem repositorySystem;

    private RepositorySystemSession repositorySystemSession;

    private List<RemoteRepository> remoteRepositories;

    private Aether(RepositorySystem repositorySystem,
                   RepositorySystemSession session, List<RemoteRepository> remoteRepositories) {
        this.repositorySystem = repositorySystem;
        this.repositorySystemSession = session;
        this.remoteRepositories = remoteRepositories;
    }

    /** Given an artifacts requests a version range for it. */
    List<String> requestVersionRange(Artifact artifact) throws VersionRangeResolutionException {
        VersionRangeRequest rangeRequest = new VersionRangeRequest(artifact, remoteRepositories, null);
        VersionRangeResult result = repositorySystem.resolveVersionRange(repositorySystemSession, rangeRequest);
        return result.getVersions().stream().map(Version::toString).collect(toList());
    }

    /** TODO(petros): this is a hack until I replace the existing Resolver. */
    static Aether defaultOption() {
        return new Aether.Builder().build();
    }

    static Aether.Builder builder() {
        return new Aether.Builder();
    }

    /** Builder class for convenience and flexibility. */
    static class Builder {

        private List<RemoteRepository> remoteRepositories;
        private RepositorySystem repositorySystem;
        private RepositorySystemSession repositorySystemSession;

        Builder() {
            remoteRepositories = Lists.newArrayList();
            repositorySystem = Utilities.newRepositorySystem();
            repositorySystemSession = Utilities.newRepositorySession(repositorySystem);
        }

        Builder remoteRepos(List<RemoteRepository> remoteRepositories) {
            this.remoteRepositories = remoteRepositories;
            return this;
        }

        Aether build() {
            return new Aether(repositorySystem, repositorySystemSession, remoteRepositories);
        }
    }

    /**
     * A set of utility methods for creating repository systems, repository system sessions and
     * remote repositories.
     */
    static class Utilities {

        /* Creates a new aether repository system. */
        static RepositorySystem newRepositorySystem() {
            DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
            locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class, FileTransporterFactory.class);
            locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
            return locator.getService(RepositorySystem.class);
        }

        /**
         * Aether and its components are designed to be stateless. All configurations and state
         * are provided through the RepositorySystemSession.
         *
         * TODO(petros): Separate this out into its own class.
         * This is the most intricate element of Aether.
         * There are various settings that are useful to us.
         * Specifically, these are the (1) LocalRepositoryManager, (2) DependencyManager,
         * (3) DependencyGraphTransformer, (4) TransferListener, (5) ProxySelector
         */
        static RepositorySystemSession newRepositorySession(RepositorySystem system) {
            DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

            // TODO(petros): Decide on where to cache things.
            LocalRepository localRepository = new LocalRepository("target/local-repo");
            session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));

            session.setDependencyManager(new ClassicDependencyManager());
            return session;
        }
    }

}
