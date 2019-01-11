package net.evendanan.bazel.mvn.merger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.evendanan.bazel.mvn.api.Dependency;

public class VerifyNoConflictingVersions {

    static Collection<Dependency> checkNoConflictingVersions(final Collection<Dependency> dependencies) {
        final Map<String, String> pinnedVersions = new HashMap<>();

        GraphUtils.DfsTraveller(dependencies, ((dependency, level) -> {
            final String pinnedVersion = pinnedVersions.putIfAbsent(dependencyKey(dependency), dependency.version());
            if (pinnedVersion!=null && !pinnedVersion.equals(dependency.version())) {
                throw new IllegalStateException("Dependency " + dependency.mavenCoordinates() + " is pinned to " + pinnedVersion + " but needed " + dependency.version());
            }
        }));

        return dependencies;
    }

    private static String dependencyKey(Dependency dependency) {
        return String.format(Locale.US, "%s:%s", dependency.groupId(), dependency.artifactId());
    }
}
