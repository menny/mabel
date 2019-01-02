package com.google.devtools.bazel.workspace.maven.adapter;

import com.google.common.base.Preconditions;
import com.google.devtools.bazel.workspace.maven.DefaultModelResolver;
import com.google.devtools.bazel.workspace.maven.MigrationToolingMavenResolver;
import com.google.devtools.bazel.workspace.maven.Rule;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.GraphResolver;
import net.evendanan.timing.TaskTiming;
import net.evendanan.timing.TimingData;
import org.apache.maven.model.Repository;

public class MigrationToolingGraphResolver implements GraphResolver {
    private final static Logger logger = Logger.getLogger("Resolver");

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
    public Collection<Dependency> resolve(final Collection<String> repositoriesUrls, final Collection<String> mavenCoordinates, final Collection<String> excludes) {
        MigrationToolingMavenResolver resolver = new MigrationToolingMavenResolver(
                new DefaultModelResolver(buildRepositories(repositoriesUrls)),
                excludes);


        final TaskTiming timer = new TaskTiming();
        List<Rule> rules = new ArrayList<>();
        logger.info(String.format("Processing %s root artifacts...", mavenCoordinates.size()));

        for (final String artifact : mavenCoordinates) {
            resolver.createRule(artifact).ifPresent(rules::add);
        }

        timer.start(mavenCoordinates.size());
        for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
            final Rule rule = rules.get(ruleIndex);
            final TimingData timingData = timer.taskDone();
            final String estimatedTimeLeft;
            if (ruleIndex >= 3) {
                estimatedTimeLeft = String.format(Locale.US, ", %s left", TaskTiming.humanReadableTime(timingData.estimatedTimeLeft));
            } else {
                estimatedTimeLeft = "";
            }
            System.out.println(
                    String.format(Locale.US, "** Resolving dependency graph for artifact %d out of %d (%.2f%%%s): %s...",
                            timingData.doneTasks, timingData.totalTasks, 100 * timingData.ratioOfDone, estimatedTimeLeft,
                            rule.mavenCoordinates()));
            resolver.resolveRuleArtifacts(rule);
        }

        return resolver.getRules().stream().map(RuleToDependency::from).collect(Collectors.toList());
    }
}
