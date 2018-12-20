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

package com.google.devtools.bazel.workspace;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Preconditions;
import com.google.devtools.bazel.workspace.maven.DefaultModelResolver;
import com.google.devtools.bazel.workspace.maven.Resolver;
import com.google.devtools.bazel.workspace.maven.Rule;
import com.google.devtools.bazel.workspace.output.BzlWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import net.evendanan.bazel.mvn.RuleClassifiers;
import org.apache.maven.model.Repository;

/**
 * Generates a WORKSPACE file for Bazel from other types of dependency trackers.
 */
public class GenerateWorkspace {

    private final static Logger logger = Logger.getLogger("GenerateWorkspace");

    private final Resolver resolver;
    private final BzlWriter resultWriter;

    public static void main(String[] args) {
        GenerateWorkspaceOptions options = new GenerateWorkspaceOptions();
        JCommander optionParser = JCommander.newBuilder().addObject(options).build();
        try {
            optionParser.parse(args);
        } catch (ParameterException e) {
            System.err.println("Unable to parse options: " + e.getLocalizedMessage());
            optionParser.usage();
            return;
        }
        if (options.artifacts.isEmpty()) {
            optionParser.usage();
            return;
        }
        if (options.repositories.isEmpty()) {
            optionParser.usage();
            return;
        }

        GenerateWorkspace workspaceFileGenerator = new GenerateWorkspace(args, options.blacklist, options.repositories, options.rules_prefix, options.macro_prefix);
        workspaceFileGenerator.generateFromArtifacts(options.artifacts);
        workspaceFileGenerator.writeResults();
    }

    private GenerateWorkspace(String[] args, List<String> blacklist, List<String> repositories, String rulePrefix, String macroPrefix) {
        this.resolver = new Resolver(new DefaultModelResolver(buildRepositories(repositories)), blacklist, rulePrefix + "___");
        this.resultWriter = new BzlWriter(args, macroPrefix);
    }

    private static List<Repository> buildRepositories(List<String> repositories) {
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

    private void generateFromArtifacts(List<String> artifacts) {
        List<Rule> rules = new ArrayList<>();
        logger.info(String.format("Processing %s root artifacts...", artifacts.size()));

        for (final String artifact : artifacts) {
            resolver.createRule(artifact).ifPresent(rules::add);
        }

        final long startTime = System.currentTimeMillis();
        for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
            final Rule rule = rules.get(ruleIndex);
            final float ratioOfDone = ruleIndex / (float) artifacts.size();
            final String estimatedTimeLeft;
            if (ruleIndex >= 10) {
                final long duration = System.currentTimeMillis() - startTime;
                estimatedTimeLeft = String.format(", %s left", humanReadableTime((long) (duration / ratioOfDone) - duration));
            } else {
                estimatedTimeLeft = "";
            }
            System.out.println(
                String.format(Locale.US, "** Processing rule %d out of %d (%.2f%%%s): %s...", (ruleIndex + 1), rules.size(), 100 * ratioOfDone, estimatedTimeLeft,
                    rule.mavenGeneratedName()));
            resolver.resolveRuleArtifacts(rule);
        }
    }

    private static String humanReadableTime(long milliseconds) {
        final long secondsInMilli = 1000;
        final long minutesInMilli = secondsInMilli * 60;

        String timeString = "";

        long elapsedMinutes = milliseconds / minutesInMilli;
        milliseconds = milliseconds % minutesInMilli;
        if (elapsedMinutes > 0) {
            timeString += elapsedMinutes + " minutes and ";
        }
        long elapsedSeconds = milliseconds / secondsInMilli;
        timeString += elapsedSeconds + " seconds";

        return timeString;
    }

    private void writeResults() {
        resultWriter.write(resolver.getRules(), RuleClassifiers::ruleClassifier, "generate_workspace.bzl");
    }

}
