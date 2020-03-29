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

package com.google.devtools.bazel.workspace.maven;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.VersionRangeResolutionException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.devtools.bazel.workspace.maven.ArtifactBuilder.InvalidArtifactCoordinateException;

/**
 * Given a Maven coordinate with a version specification resolves the version of the coordinate in a
 * similar fashion as Maven. Version specifications can include hard and soft pins as well as
 * various forms of version ranges. When given a version range, Maven selects the highest available
 * version. For a soft pin, it selects the pinned version or the nearest valid version.
 *
 * <p>Documentation on Maven's versioning scheme can be found here:
 * http://maven.apache.org/enforcer/enforcer-rules/versionRanges.html
 */
public class VersionResolver {

    /**
     * Given a maven coordinate and its version specifications, selects the highest version if it is
     * a version range or returns the pinned version if is a hard or soft pin. For soft pins, if
     * that version does not exist it selects the nearest version.
     */
    private static final Pattern VERSION_SPEC_CONTAINER = Pattern.compile("^[\\[(].+[)\\]]$");

    private static final Pattern VERSION_SPEC_ITEM = Pattern.compile("([\\w.]+)");
    private final boolean debugLogs;
    private final Aether aether;

    private VersionResolver(Aether aether, boolean debugLogs) {
        this.aether = aether;
        this.debugLogs = debugLogs;
    }

    /** default error message */
    private static String messageForInvalidArtifact(
            String groupId,
            String artifactId,
            String versionSpec,
            String classifier,
            String errorMessage) {
        return String.format(
                "Unable to find a version for %s:%s:%s (with classifier '%s') due to %s",
                groupId, artifactId, versionSpec, classifier, errorMessage);
    }

    /** Creates a VersionResolver with the default Aether settings. */
    public static VersionResolver defaultResolver(boolean debugLogs) {
        return new VersionResolver(Aether.defaultOption(), debugLogs);
    }

    String resolveVersion(String groupId, String artifactId, String classifier, String versionSpec)
            throws InvalidArtifactCoordinateException {

        if (VERSION_SPEC_CONTAINER.matcher(versionSpec).matches()) {
            List<String> versions;
            try {
                versions = requestVersionList(groupId, artifactId, classifier, versionSpec);
            } catch (VersionRangeResolutionException e) {
                String errorMessage =
                        messageForInvalidArtifact(
                                groupId, artifactId, versionSpec, classifier, e.getMessage());
                throw new InvalidArtifactCoordinateException(errorMessage, e);
            }

            if (versions == null || versions.isEmpty()) {
                final Matcher versionItemMatcher = VERSION_SPEC_ITEM.matcher(versionSpec);
                if (versionItemMatcher.find()) {
                    final String defaultVersion = versionItemMatcher.group(1);
                    if (debugLogs)
                        System.out.println(
                                "Failed to resolve version for spec "
                                        + versionSpec
                                        + ". Returning "
                                        + defaultVersion);
                    return defaultVersion;
                } else {
                    if (debugLogs)
                        System.out.println(
                                "Failed to resolve version for spec "
                                        + versionSpec
                                        + " and could not identify range. Returning "
                                        + versionSpec);
                    return versionSpec;
                }
            }

            if (debugLogs)
                System.out.println(
                        "Found valid versions for spec "
                                + versionSpec
                                + ": "
                                + String.join(",", versions));

            return versions.get(versions.size() - 1);
        } else {
            // well... not really a spec, just plain version
            return versionSpec;
        }
    }

    /** Given a set of maven coordinates, obtains a list of valid versions in ascending order. */
    private List<String> requestVersionList(
            String groupId, String artifactId, String classifier, String versionSpec)
            throws VersionRangeResolutionException {
        Artifact artifact =
                ArtifactBuilder.fromCoords(groupId, artifactId, classifier, versionSpec);
        return aether.requestVersionRange(artifact);
    }
}
