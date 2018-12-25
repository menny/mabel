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
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.devtools.bazel.workspace.maven.DefaultModelResolver;
import com.google.devtools.bazel.workspace.maven.Resolver;
import com.google.devtools.bazel.workspace.maven.Rule;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import net.evendanan.bazel.mvn.RuleClassifiers;
import net.evendanan.bazel.mvn.RuleWriter;
import net.evendanan.bazel.mvn.RuleWriters;
import net.evendanan.timing.TaskTiming;
import net.evendanan.timing.TimingData;
import org.apache.maven.model.Repository;

/**
 * Generates a WORKSPACE file for Bazel from other types of dependency trackers.
 */
public class GenerateWorkspace {

    private final static Logger logger = Logger.getLogger("GenerateWorkspace");

    private final Resolver resolver;
    private final RuleWriter repositoryRulesWriter;
    private final RuleWriter targetRulesWriter;
    private final File outputFile = new File("generate_workspace.bzl");

    public static void main(String[] args) throws Exception {
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
        workspaceFileGenerator.writeResults(args);
    }

    private GenerateWorkspace(String[] args, List<String> blacklist, List<String> repositories, String rulePrefix, String macroPrefix) {
        this.resolver = new Resolver(new DefaultModelResolver(buildRepositories(repositories)), blacklist, rulePrefix + "___");
        this.repositoryRulesWriter = new RuleWriters.HttpRepoRulesMacroWriter(
            outputFile,
            String.format(Locale.US, "generate_%s_workspace_rules", macroPrefix));
        this.targetRulesWriter = new RuleWriters.TransitiveRulesMacroWriter(
            outputFile,
            String.format(Locale.US, "generate_%s_transitive_dependency_rules", macroPrefix),
            RuleClassifiers.NATIVE_RULE_MAPPER);
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
        final TaskTiming timer = new TaskTiming();
        List<Rule> rules = new ArrayList<>();
        logger.info(String.format("Processing %s root artifacts...", artifacts.size()));

        for (final String artifact : artifacts) {
            resolver.createRule(artifact).ifPresent(rules::add);
        }

        timer.start();
        timer.setTotalTasks(artifacts.size());
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
                String.format(Locale.US, "** Processing rule %d out of %d (%.2f%%%s): %s...",
                    timingData.doneTasks, timingData.totalTasks, 100 * timingData.ratioOfDone, estimatedTimeLeft,
                    rule.mavenGeneratedName()));
            resolver.resolveRuleArtifacts(rule);
        }
    }

    private void writeResults(final String[] args) throws Exception {
        try (final OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(outputFile, false), Charsets.UTF_8)) {
            fileWriter.append("# Auto-generated by https://github.com/menny/bazel-mvn-deps").append(System.lineSeparator());
            fileWriter.append(System.lineSeparator());
            fileWriter.append("# Args: ").append(String.join(" ", args)).append(System.lineSeparator());
            fileWriter.append(System.lineSeparator());
            fileWriter.append(System.lineSeparator());
        }
        repositoryRulesWriter.write(resolver.getRules());
        targetRulesWriter.write(resolver.getRules());
    }

}
