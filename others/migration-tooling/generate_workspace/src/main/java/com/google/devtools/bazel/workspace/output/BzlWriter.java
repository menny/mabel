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

import static com.google.devtools.bazel.workspace.output.RuleFormatters.HTTP_FILE;
import static com.google.devtools.bazel.workspace.output.RuleFormatters.RULE_INDENT;

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

/**
 * Writes WORKSPACE and BUILD file definitions to a .bzl file.
 */
public class BzlWriter {

    private final static Logger logger = Logger.getLogger(
        MethodHandles.lookup().lookupClass().getName());

    private final String[] argv;
    private final String macrosPrefix;

    public BzlWriter(String[] argv, String macrosPrefix) {
        this.argv = argv;
        this.macrosPrefix = macrosPrefix;
    }

    public void write(Collection<Rule> rules, String outputFile) {
        final Path generatedFile = Paths.get(outputFile);
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
                outputStream.println(HTTP_FILE.formatRule(rule));
                outputStream.println();
            }
        }

        outputStream.println();

        outputStream.println("# Transitive rules macro to be run in the BUILD.bazel file.");
        outputStream.print("def generate_");
        outputStream.print(macrosPrefix);
        outputStream.println("_transitive_dependency_rules():");
        if (noRules) {
            outputStream.print(RULE_INDENT);
            outputStream.println("pass");
        } else {
            rules.forEach(rule -> {
                outputStream.println(RuleClassifiers.ruleClassifier(rule).formatRule(rule));
                outputStream.println();
            });
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
