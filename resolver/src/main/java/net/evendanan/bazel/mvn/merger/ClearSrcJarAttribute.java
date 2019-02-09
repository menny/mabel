package net.evendanan.bazel.mvn.merger;

import java.net.URI;
import java.util.Collection;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;

public class ClearSrcJarAttribute {
    public static Collection<Dependency> clearSrcJar(final Collection<Dependency> dependencies) {
        return dependencies.stream()
                .map(dependency -> new Dependency(dependency.groupId(), dependency.artifactId(), dependency.version(), dependency.packaging(),
                        clearSrcJar(dependency.dependencies()),
                        clearSrcJar(dependency.exports()),
                        clearSrcJar(dependency.runtimeDependencies()),
                        dependency.url(), URI.create(""), dependency.javadocUrl(), dependency.licenses()))
                .collect(Collectors.toList());
    }
}
