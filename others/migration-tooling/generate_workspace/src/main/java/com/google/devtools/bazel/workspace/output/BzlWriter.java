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

import com.google.common.annotations.VisibleForTesting;
import com.google.devtools.bazel.workspace.maven.Rule;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Writes WORKSPACE and BUILD file definitions to a .bzl file.
 */
public class BzlWriter extends AbstractWriter {

    private final static Logger logger = Logger.getLogger(
        MethodHandles.lookup().lookupClass().getName());

    private final String[] argv;
    private final Path generatedFile;
    private final String macrosPrefix;

    public BzlWriter(String[] argv, String outputDirStr, String macrosPrefix) {
        this.argv = argv;
        this.generatedFile = Paths.get(outputDirStr).resolve("generate_workspace.bzl");
        this.macrosPrefix = macrosPrefix;
    }

    @Override
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
        Collection<Rule> javaImportRules = rules.stream().filter(rule -> !"aar".equals(rule.packaging())).collect(Collectors.toList());
        Collection<Rule> aarRules = rules.stream().filter(rule -> "aar".equals(rule.packaging())).collect(Collectors.toList());

        writeHeader(outputStream, argv);

        outputStream.println("load('@bazel_tools//tools/build_defs/repo:http.bzl', 'http_file')");
        outputStream.println();

        // flat workspace rules
        outputStream.print("def generate_");
        outputStream.print(macrosPrefix);
        outputStream.println("_workspace_rules():");
        if (javaImportRules.isEmpty() && aarRules.isEmpty()) {
            outputStream.println("  pass\n");
        }

        // jar http files
        for (Rule rule : javaImportRules) {
            outputStream.println(formatHttpFile(rule, "  "));
        }
        outputStream.append("\n\n");

        // aar http files
        for (Rule rule : aarRules) {
            outputStream.println(formatHttpFile(rule, "  "));
        }
        outputStream.append("\n\n");

        // transitive dependency rules
        outputStream.print("def generate_");
        outputStream.print(macrosPrefix);
        outputStream.println("_transitive_dependency_rules():");
        if (javaImportRules.isEmpty() && aarRules.isEmpty()) {
            outputStream.println("  pass\n");
        }

        // transitive java import rules
        for (Rule rule : javaImportRules) {
            outputStream.println(formatJavaImport(rule, "  "));
        }
        outputStream.append("\n\n");

        // transitive aar import rules
        for (Rule rule : aarRules) {
            outputStream.println(formatAarImport(rule, "  "));
        }

    }

    /** Creates parent directories if they don't exist */
    @VisibleForTesting
    void createParentDirectory(Path generatedFile) throws IOException {
        Path parentDirectory = generatedFile.toAbsolutePath().getParent();
        if (Files.exists(parentDirectory)) {
            return;
        }
        Files.createDirectories(parentDirectory);
    }
}
