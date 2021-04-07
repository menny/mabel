package com.google.devtools.bazel.workspace.maven.adapter;

import com.google.common.base.Preconditions;
import com.google.devtools.bazel.workspace.maven.DefaultModelResolver;
import com.google.devtools.bazel.workspace.maven.MigrationToolingMavenResolver;
import com.google.devtools.bazel.workspace.maven.Rule;
import com.google.devtools.bazel.workspace.maven.VersionResolver;
import net.evendanan.bazel.mvn.api.GraphResolver;
import net.evendanan.bazel.mvn.api.model.Resolution;
import org.apache.maven.model.Repository;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MigrationToolingGraphResolver implements GraphResolver {

    private final boolean debugLogs;
    private final String jdkHome;

    public MigrationToolingGraphResolver(boolean debugLogs, String jdkHome) {
        this.debugLogs = debugLogs;
        this.jdkHome = jdkHome;
    }

    private static List<Repository> buildRepositories(Collection<String> repositories) {
        ArrayList<Repository> repositoryList = new ArrayList<>(repositories.size());
        for (String repositoryUrlString : repositories) {
            Preconditions.checkState(
                    repositoryUrlString.endsWith("/"),
                    "Repository url '%s' should end with '/'",
                    repositoryUrlString);
            final Repository repository = new Repository();
            URI repositoryUri = URI.create(repositoryUrlString);
            repository.setId(repositoryUri.getHost());
            repository.setName(repositoryUri.getHost());
            repository.setUrl(repositoryUrlString);
            repositoryList.add(repository);
        }

        return repositoryList;
    }

    @Override
    public Resolution resolve(
            String mavenCoordinate,
            final Collection<String> repositoriesUrls,
            final Collection<String> excludes) {
        final List<Repository> repositories = buildRepositories(repositoriesUrls);
        final VersionResolver versionResolver = VersionResolver.defaultResolver(debugLogs);
        MigrationToolingMavenResolver resolver =
                new MigrationToolingMavenResolver(
                        repositories,
                        new DefaultModelResolver(repositories, versionResolver, jdkHome),
                        versionResolver,
                        excludes,
                        debugLogs);

        final int packagingIndex = mavenCoordinate.indexOf('@');
        String packaging = null;
        if (packagingIndex > 0) {
            packaging = mavenCoordinate.substring(packagingIndex + 1);
            mavenCoordinate = mavenCoordinate.substring(0, packagingIndex);
        }

        final Rule rule = resolver.resolveRuleArtifacts(mavenCoordinate);
        if (packaging != null) {
            rule.setPackaging(packaging);
        }

        return new RuleToDependency(debugLogs).from(rule);
    }
}
