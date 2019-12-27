package net.evendanan.bazel.mvn.merger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;

public class VerifyNoConflictingVersions {

    static Collection<Dependency> checkNoConflictingVersions(final Collection<Dependency> dependencies) {
        final Map<String, String> pinnedVersions = new HashMap<>();

        GraphUtils.DfsTraveller(dependencies, ((dependency, level) -> {
            final String pinnedVersion = pinnedVersions.putIfAbsent(dependencyKey(dependency), dependency.getVersion());
            if (pinnedVersion!=null && !pinnedVersion.equals(dependency.getVersion())) {
                throw new IllegalStateException("Dependency " + DependencyTools.DEFAULT.mavenCoordinates(dependency) + " is pinned to " + pinnedVersion + " but needed " + dependency.getVersion());
            }
        }));

        return dependencies;
    }

    private static String dependencyKey(Dependency dependency) {
        return String.format(Locale.US, "%s:%s", dependency.getGroupId(), dependency.getArtifactId());
    }
}
