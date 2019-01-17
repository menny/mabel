package com.google.devtools.bazel.workspace.maven.adapter;

import com.google.common.base.Preconditions;
import com.google.devtools.bazel.workspace.maven.DefaultModelResolver;
import com.google.devtools.bazel.workspace.maven.MigrationToolingMavenResolver;
import com.google.devtools.bazel.workspace.maven.Rule;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.GraphResolver;
import org.apache.maven.model.Repository;

public class MigrationToolingGraphResolver implements GraphResolver {
    private static List<Repository> buildRepositories(Collection<String> repositories) {
        ArrayList<Repository> repositoryList = new ArrayList<>(repositories.size());
        for (String repositoryUrlString : repositories) {
            Preconditions.checkState(repositoryUrlString.endsWith("/"), "Repository url '%s' should end with '/'", repositoryUrlString);
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
    public Dependency resolve(String mavenCoordinate, final Collection<String> repositoriesUrls, final Collection<String> excludes) {
        MigrationToolingMavenResolver resolver = new MigrationToolingMavenResolver(
                new DefaultModelResolver(buildRepositories(repositoriesUrls)),
                excludes);


        final Rule rule = resolver.createRule(mavenCoordinate).orElseThrow(() -> new IllegalArgumentException("Illegal Maven coordinates " + mavenCoordinate));
        resolver.resolveRuleArtifacts(rule);

        return RuleToDependency.from(rule);
    }
}
