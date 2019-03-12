package net.evendanan.bazel.mvn.api;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String packaging;

    private final List<Dependency> dependencies;
    private final List<Dependency> exports;
    private final List<Dependency> runtimeDependencies;

    private final URI url;

    private final URI sourcesUrl;
    private final URI javadocUrl;
    private final Collection<License> licenses;

    public Dependency(final String groupId, final String artifactId, final String version, final String packaging, final Collection<Dependency> dependencies, final Collection<Dependency> exports, final Collection<Dependency> runtimeDependencies,
                      final URI url,
                      final URI sourcesUrl,
                      final URI javadocUrl,
                      final Collection<License> licenses) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
        this.dependencies = ImmutableList.copyOf(dependencies);
        this.exports = ImmutableList.copyOf(exports);
        this.runtimeDependencies = ImmutableList.copyOf(runtimeDependencies);
        this.url = url;
        this.sourcesUrl = sourcesUrl;
        this.javadocUrl = javadocUrl;
        this.licenses = ImmutableList.copyOf(licenses);
    }

    private static String normalize(String name) {
        return name.replaceAll("[+.-]", "_");
    }

    public String groupId() {
        return groupId;
    }

    public String artifactId() {
        return artifactId;
    }

    public String version() {
        return version;
    }

    public String packaging() {
        return packaging;
    }

    public String mavenCoordinates() {
        return String.format(Locale.ROOT, "%s:%s:%s", groupId, artifactId, version);
    }

    public String repositoryRuleName() {
        return String.format(Locale.ROOT, "%s__%s__%s", normalize(groupId), normalize(artifactId), normalize(version));
    }

    public String targetName() {
        return String.format(Locale.ROOT, "%s__%s", normalize(groupId), normalize(artifactId));
    }

    public Collection<Dependency> dependencies() {
        return dependencies;
    }

    public Collection<Dependency> exports() {
        return exports;
    }

    public Collection<Dependency> runtimeDependencies() {
        return runtimeDependencies;
    }

    public URI url() {
        return url;
    }

    public URI sourcesUrl() {
        return sourcesUrl;
    }

    public URI javadocUrl() {
        return javadocUrl;
    }

    public Collection<License> licenses() {
        return licenses;
    }

    @Override
    public String toString() {
        return mavenCoordinates();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Dependency) {
            Dependency other = (Dependency) obj;
            return other.mavenCoordinates().equals(mavenCoordinates());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return mavenCoordinates().hashCode();
    }

}
