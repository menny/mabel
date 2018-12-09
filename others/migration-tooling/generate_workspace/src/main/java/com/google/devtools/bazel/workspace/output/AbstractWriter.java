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
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Basic implementation for output writers.
 */
public abstract class AbstractWriter {

    public abstract void write(Collection<Rule> rules);

    /**
     * Writes the list of sources as a comment to outputStream.
     */
    void writeHeader(PrintStream outputStream, String[] argv) {
        outputStream.println("# The following dependencies were calculated from:");
        outputStream.println("#");
        outputStream.println("# generate_workspace " + String.join(" ", argv));
        outputStream.print("\n\n");
    }

    String formatHttpFile(Rule rule, String indent) {
        StringBuilder builder = new StringBuilder(63);
        for (String parent : rule.getParents()) {
            builder.append(indent).append("# ").append(parent).append('\n');
        }
        builder.append(indent).append("http_file").append("(\n");
        builder.append(indent).append("    name = \"").append(rule.safeRuleFriendlyName()).append("\",\n");
        builder.append(indent).append("    urls = [\"").append(rule.getUrl()).append("\"],\n");
        builder.append(indent).append("    downloaded_file_path = \"").append(getFilenameFromUrl(rule.getUrl())).append("\",\n");
        builder.append(indent).append(")\n\n");
        return builder.toString();
    }

    private static String getFilenameFromUrl(String url) {
        int lastPathSeparator = url.lastIndexOf("/");
        if (lastPathSeparator < 0) throw new IllegalArgumentException("Could not parse filename out of URL '" + url + "'");

        return url.substring(lastPathSeparator + 1);
    }

    /**
     * Write library rules to depend on the transitive closure of all of these rules.
     */
    String formatJavaImport(Rule rule, String indent) {
        StringBuilder builder = new StringBuilder(241);
        builder.append(indent).append("native.java_import").append("(\n");
        builder.append(indent).append("    name = \"").append(rule.mavenGeneratedName()).append("\",\n");
        builder.append(indent).append("    visibility = [\"//visibility:public\"],\n");

        builder.append(indent).append("    jars = [\"@").append(rule.safeRuleFriendlyName()).append("//file\"],\n");

        final Set<Rule> deps = rule.getDeps();
        if (!deps.isEmpty()) {
            builder.append(indent).append("    deps = [\n");
            for (Rule r : deps) {
                builder.append(indent).append("        \":").append(r.safeRuleFriendlyName()).append("\",\n");
            }
            builder.append(indent).append("    ],\n");
        }

        final Set<Rule> exportDeps = rule.getExportDeps();
        if (!exportDeps.isEmpty()) {
            builder.append(indent).append("    exports = [\n");
            for (Rule r : exportDeps) {
                builder.append(indent).append("        \":").append(r.safeRuleFriendlyName()).append("\",\n");
            }
            builder.append(indent).append("    ],\n");
        }

        final Set<Rule> runtimeDeps = rule.getRuntimeDeps();
        if (!runtimeDeps.isEmpty()) {
            builder.append(indent).append("    runtime_deps = [\n");
            for (Rule r : runtimeDeps) {
                builder.append(indent).append("        \":").append(r.safeRuleFriendlyName()).append("\",\n");
            }
            builder.append(indent).append("    ],\n");
        }
        builder.append(indent).append(")\n");

        //generating an alias for this rule
        builder.append(indent).append("native.alias(\n");
        builder.append(indent).append("    name = \"").append(rule.safeRuleFriendlyName()).append("\",\n");
        builder.append(indent).append("    actual = \"").append(rule.mavenGeneratedName()).append("\",\n");
        builder.append(indent).append("    visibility = [\"//visibility:public\"],\n");
        builder.append(indent).append(")\n\n");

        return builder.toString();
    }

    /**
     * Write library rules to depend on the transitive closure of all of these rules.
     */
    String formatAarImport(Rule rule, String indent) {
        StringBuilder builder = new StringBuilder(229);
        builder.append(indent).append("native.aar_import").append("(\n");
        builder.append(indent).append("    name = \"").append(rule.mavenGeneratedName()).append("\",\n");
        builder.append(indent).append("    visibility = [\"//visibility:public\"],\n");
        builder.append(indent).append("    aar = \"@").append(rule.safeRuleFriendlyName()).append("//file\",\n");

        final Set<Rule> deps = new HashSet<>();
        deps.addAll(rule.getDeps());
        deps.addAll(rule.getRuntimeDeps());
        if (!deps.isEmpty()) {
            builder.append(indent).append("    deps = [\n");
            for (Rule r : deps) {
                builder.append(indent).append("        \":").append(r.safeRuleFriendlyName()).append("\",\n");
            }
            builder.append(indent).append("    ],\n");
        }

        final Set<Rule> exportDeps = rule.getExportDeps();
        if (!exportDeps.isEmpty()) {
            builder.append(indent).append("    exports = [\n");
            for (Rule r : exportDeps) {
                builder.append(indent).append("        \":").append(r.safeRuleFriendlyName()).append("\",\n");
            }
            builder.append(indent).append("    ],\n");
        }
        builder.append(indent).append(")\n");

        //generating an alias for this rule
        builder.append(indent).append("native.alias(\n");
        builder.append(indent).append("    name = \"").append(rule.safeRuleFriendlyName()).append("\",\n");
        builder.append(indent).append("    actual = \"").append(rule.mavenGeneratedName()).append("\",\n");
        builder.append(indent).append("    visibility = [\"//visibility:public\"],\n");
        builder.append(indent).append(")\n\n");

        return builder.toString();
    }
}
