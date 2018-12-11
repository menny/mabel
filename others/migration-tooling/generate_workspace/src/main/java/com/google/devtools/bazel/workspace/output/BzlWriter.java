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

package com.google.devtools.bazel.workspace.output;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Writes WORKSPACE and BUILD file definitions to a .bzl file.
 */
public class BzlWriter {

    private final static Logger logger = Logger.getLogger(
        MethodHandles.lookup().lookupClass().getName());

    private static final String RULE_INDENT = "    ";
    private static final String RULE_ARGUMENTS_INDENT = RULE_INDENT + RULE_INDENT;

    private final String[] argv;
    private final Path generatedFile;
    private final String macrosPrefix;

    public BzlWriter(String[] argv, String outputDirStr, String macrosPrefix) {
        this.argv = argv;
        this.generatedFile = Paths.get(outputDirStr).resolve("generate_workspace.bzl");
        this.macrosPrefix = macrosPrefix;
    }

    /**
     * Writes the list of sources as a comment to outputStream.
     */
    private static void writeHeader(PrintStream outputStream, String[] argv) {
        outputStream.println("# The following dependencies were calculated from:");
        outputStream.println("#");
        outputStream.println("# generate_workspace " + String.join(" ", argv));
        outputStream.println();
        outputStream.println();
    }

    private static String formatHttpFile(Rule rule) {
        StringBuilder builder = new StringBuilder(63);
        for (String parent : rule.getParents()) {
            builder.append(RULE_INDENT).append("# ").append(parent).append('\n');
        }
        builder.append(RULE_INDENT).append("http_file").append("(\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.safeRuleFriendlyName()).append("\",\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("urls = [\"").append(rule.getUrl()).append("\"],\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("downloaded_file_path = \"").append(getFilenameFromUrl(rule.getUrl())).append("\",\n");
        builder.append(RULE_ARGUMENTS_INDENT).append(")");
        return builder.toString();
    }

    private static String getFilenameFromUrl(String url) {
        int lastPathSeparator = url.lastIndexOf("/");
        if (lastPathSeparator < 0) {
            throw new IllegalArgumentException("Could not parse filename out of URL '" + url + "'");
        }

        return url.substring(lastPathSeparator + 1);
    }

    /**
     * Write library rules to depend on the transitive closure of all of these rules.
     */
    private String formatJavaImport(Rule rule) {
        StringBuilder builder = new StringBuilder(241);
        builder.append(RULE_INDENT).append("native.java_import").append("(\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.mavenGeneratedName()).append("\",\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("jars = [\"@").append(rule.safeRuleFriendlyName()).append("//file\"],\n");
        addListArgument(builder, "deps", rule.getDeps());
        addListArgument(builder, "exports", rule.getExportDeps());
        addListArgument(builder, "runtime_deps", rule.getRuntimeDeps());

        builder.append(RULE_INDENT).append(")\n");

        addAlias(builder, rule);

        return builder.toString();
    }

    private static String formatAarImport(Rule rule) {
        StringBuilder builder = new StringBuilder(229);
        builder.append(RULE_INDENT).append("native.aar_import").append("(\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.mavenGeneratedName()).append("\",\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("aar = \"@").append(rule.safeRuleFriendlyName()).append("//file\",\n");

        final Set<Rule> deps = new HashSet<>();
        deps.addAll(rule.getDeps());
        deps.addAll(rule.getRuntimeDeps());
        addListArgument(builder, "deps", deps);
        addListArgument(builder, "exports", rule.getExportDeps());

        builder.append(RULE_INDENT).append(")\n");

        addAlias(builder, rule);

        return builder.toString();
    }

    private static void addListArgument(StringBuilder builder, String name, Collection<Rule> labels) {
        if (!labels.isEmpty()) {
            builder.append(RULE_ARGUMENTS_INDENT).append(name).append(" = [\n");
            for (Rule r : labels) {
                builder.append(RULE_ARGUMENTS_INDENT).append(RULE_INDENT).append("\":").append(r.safeRuleFriendlyName()).append("\",\n");
            }
            builder.append(RULE_ARGUMENTS_INDENT).append("],\n");
        }
    }

    private static void addAlias(StringBuilder builder, Rule rule) {
        builder.append(RULE_INDENT).append("native.alias(\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("name = \"").append(rule.safeRuleFriendlyName()).append("\",\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("actual = \"").append(rule.mavenGeneratedName()).append("\",\n");
        builder.append(RULE_ARGUMENTS_INDENT).append("visibility = [\"//visibility:public\"],\n");
        builder.append(RULE_INDENT).append(")\n\n");
    }

    public void write(Collection<Rule> rules) {
        try {
            createParentDirectory(generatedFile);
        } catch (IOException | NullPointerException e) {
            logger.severe("Could not create directories for " + generatedFile + ": " + e.getMessage());
            return;
        }
        try (PrintStream outputStream = new PrintStream(generatedFile.toFile())) {
            writeBzl(outputStream, rules);
        } catch (FileNotFoundException e) {
            logger.severe("Could not write " + generatedFile + ": " + e.getMessage());
            return;
        }
        System.err.println("Wrote " + generatedFile.toAbsolutePath());
    }

    private void writeBzl(PrintStream outputStream, Collection<Rule> rules) {
        writeHeader(outputStream, argv);

        outputStream.println("# Loading a drop-in replacement for native.http_file");
        outputStream.println("load('@bazel_tools//tools/build_defs/repo:http.bzl', 'http_file')");
        outputStream.println();

        final boolean noRules = rules.isEmpty();

        outputStream.println("# Repository rules macro to be run in the WORKSPACE file.");
        outputStream.print("def generate_");
        outputStream.print(macrosPrefix);
        outputStream.println("_workspace_rules():");
        if (noRules) {
            outputStream.print(RULE_INDENT);
            outputStream.println("pass");
        } else {
            for (Rule rule : rules) {
                outputStream.println(formatHttpFile(rule));
                outputStream.println();
            }
        }

        outputStream.println();

        //filtering the rules to their types
        Collection<Rule> aarRules = rules.stream().filter(rule -> "aar".equals(rule.packaging())).collect(Collectors.toList());
        rules.removeAll(aarRules);
        //whatever is left in rules, are plain java rules.

        outputStream.println("# Transitive rules macro to be run in the BUILD.bazel file.");
        outputStream.print("def generate_");
        outputStream.print(macrosPrefix);
        outputStream.println("_transitive_dependency_rules():");
        if (noRules) {
            outputStream.print(RULE_INDENT);
            outputStream.println("pass");
        } else {
            for (Rule rule : aarRules) {
                outputStream.println(formatAarImport(rule));
            }
            for (Rule rule : rules) {
                outputStream.println(formatJavaImport(rule));
            }
        }
    }

    /** Creates parent directories if they don't exist */
    private void createParentDirectory(Path generatedFile) throws IOException {
        Path parentDirectory = generatedFile.toAbsolutePath().getParent();
        if (Files.exists(parentDirectory)) {
            return;
        }
        Files.createDirectories(parentDirectory);
    }
}
