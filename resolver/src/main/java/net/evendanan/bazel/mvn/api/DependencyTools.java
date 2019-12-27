package net.evendanan.bazel.mvn.api;

import java.util.Locale;

public class DependencyTools {

    public static final DependencyTools DEFAULT = new DependencyTools();

    protected static String normalize(String name) {
        return name.replaceAll("[+.-]", "_");
    }

    public String mavenCoordinates(Dependency dependency) {
        return String.format(Locale.ROOT, "%s:%s:%s", dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    public String repositoryRuleName(Dependency dependency) {
        return String.format(Locale.ROOT, "%s__%s__%s", normalize(dependency.getGroupId()), normalize(dependency.getArtifactId()), normalize(dependency.getVersion()));
    }

    public String targetName(Dependency dependency) {
        return String.format(Locale.ROOT, "%s__%s", normalize(dependency.getGroupId()), normalize(dependency.getArtifactId()));
    }
}
