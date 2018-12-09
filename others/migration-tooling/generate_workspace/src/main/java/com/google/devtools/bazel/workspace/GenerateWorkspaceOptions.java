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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.IParameterSplitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command-line options for generate_workspace tool.
 */
@Parameters(separators = "=")
public class GenerateWorkspaceOptions {

    @Parameter(
        names = { "--artifact", "-a" },
        splitter = NoSplitter.class,
        description = "Maven artifact coordinates (e.g. groupId:artifactId:version)."
    )
    public List<String> artifacts = new ArrayList<>();

    @Parameter(
        names = { "--blacklist", "-b" },
        splitter = NoSplitter.class,
        description = "Blacklisted Maven artifact coordinates (e.g. groupId:artifactId:version)."
    )
    public List<String> blacklist = new ArrayList<>();

    @Parameter(
        names = { "--repository" },
        splitter = NoSplitter.class,
        description = "Maven repository url."
    )
    public List<String> repositories = new ArrayList<>();

    @Parameter(
        names = { "--rule_prefix" },
        description = "Prefix text to add to all rules."
    )
    public String rules_prefix = "";

    @Parameter(
        names = { "--macro_prefix" },
        description = "Prefix text to add to all macros."
    )
    public String macro_prefix = "";

    /**
     * Jcommander defaults to splitting each parameter by comma. For example,
     * --a=group:artifact:[x1,x2] is parsed as two items 'group:artifact:[x1' and 'x2]',
     * instead of the intended 'group:artifact:[x1,x2]'
     *
     * For more information: http://jcommander.org/#_splitting
     */
    private static class NoSplitter implements IParameterSplitter {

        @Override
        public List<String> split(String value) {
            return Collections.singletonList(value);
        }
    }
}
