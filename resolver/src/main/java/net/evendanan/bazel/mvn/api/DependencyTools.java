package net.evendanan.bazel.mvn.api;

import java.util.Locale;

public class DependencyTools {

    public static final DependencyTools DEFAULT = new DependencyTools();

    private static String normalize(String name) {
        return name.replaceAll("[+.-]", "_");
    }

    public final String mavenCoordinates(Dependency dependency) {
        return mavenCoordinates(dependency.getMavenCoordinate());
    }

    public String mavenCoordinates(MavenCoordinate mavenCoordinate) {
        return String.format(Locale.ROOT, "%s:%s:%s", mavenCoordinate.getGroupId(), mavenCoordinate.getArtifactId(), mavenCoordinate.getVersion());
    }

    public final String repositoryRuleName(Dependency dependency) {
        return repositoryRuleName(dependency.getMavenCoordinate());
    }

    public String repositoryRuleName(MavenCoordinate mavenCoordinate) {
        return String.format(Locale.ROOT, "%s__%s__%s", normalize(mavenCoordinate.getGroupId()), normalize(mavenCoordinate.getArtifactId()), normalize(mavenCoordinate.getVersion()));
    }

    public final String targetName(Dependency dependency) {
        return targetName(dependency.getMavenCoordinate());
    }

    public String targetName(MavenCoordinate mavenCoordinate) {
        return String.format(Locale.ROOT, "%s__%s", normalize(mavenCoordinate.getGroupId()), normalize(mavenCoordinate.getArtifactId()));
    }
}
