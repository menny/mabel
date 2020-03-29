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

import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Builds Aether/Maven artifacts */
public class ArtifactBuilder {

    /** Builds a Maven artifact from a set of Maven coordinates */
    public static Artifact fromCoords(String artifactCoords)
            throws InvalidArtifactCoordinateException {
        try {
            return new DefaultArtifact(artifactCoords);
        } catch (IllegalArgumentException e) {
            throw new InvalidArtifactCoordinateException(e.getMessage(), e);
        }
    }

    /** Builds a Maven artifact from a set of Maven coordinates */
    public static Artifact fromCoords(
            String groupId, String artifactId, String classifier, String versionSpec) {
        return new DefaultArtifact(
                groupId, artifactId, classifier, null /*packaging*/, versionSpec);
    }

    /**
     * Builds a Maven artifact from a dependency. Note, this is a org.apache.maven.model.Dependency
     * and not the Dependency defined by aether.
     */
    public static Artifact fromMavenDependency(
            Dependency dep, VersionResolver versionResolver, Model model)
            throws InvalidArtifactCoordinateException {
        final String classifier = dep.getClassifier() == null ? "" : dep.getClassifier();

        final String versionWithModelProperties =
                ProfilePlaceholderUtil.replacePlaceholders(model, dep.getVersion());
        final String version =
                versionResolver.resolveVersion(
                        dep.getGroupId(),
                        dep.getArtifactId(),
                        classifier,
                        versionWithModelProperties);

        return fromCoords(dep.getGroupId(), dep.getArtifactId(), classifier, version);
    }

    @VisibleForTesting
    static class ProfilePlaceholderUtil {
        private static final boolean DEBUG = false;

        private static final Pattern PROPERTY_PLACEHOLDER = Pattern.compile("(\\$\\{([\\w.]+)})");

        static String replacePlaceholders(final Model model, String text) {
            final Matcher matcher = PROPERTY_PLACEHOLDER.matcher(text);
            // using while, since there could be multiple placeholders
            while (matcher.find()) {
                for (int matchIndex = 1; matchIndex <= matcher.groupCount(); matchIndex += 2) {
                    final String placeholder = matcher.group(matchIndex);
                    final String placeholderKey = matcher.group(matchIndex + 1);
                    if (DEBUG)
                        System.out.println("Match for " + text + ": placeholder " + placeholder);
                    final String placeholderValue = findPropertyValue(model, placeholderKey);
                    if (placeholderValue != null) {
                        if (DEBUG)
                            System.out.println(
                                    "POM Property "
                                            + placeholderKey
                                            + " has value "
                                            + placeholderValue);
                        text = text.replace(placeholder, placeholderValue);
                    }
                }
            }

            return text;
        }

        private static String findPropertyValue(final Model model, final String placeholderKey) {
            for (final Profile profile : model.getProfiles()) {
                for (final String propertyKey : profile.getProperties().stringPropertyNames()) {
                    if (propertyKey.equals(placeholderKey)) {
                        return profile.getProperties().getProperty(propertyKey);
                    }
                }
            }

            return null;
        }
    }

    /** Exception thrown if an artifact coordinate cannot be parsed */
    public static class InvalidArtifactCoordinateException extends Exception {

        InvalidArtifactCoordinateException(String message, Throwable e) {
            super(message, e);
        }
    }
}
