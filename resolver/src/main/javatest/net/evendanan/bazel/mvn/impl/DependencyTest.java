package net.evendanan.bazel.mvn.impl;

import java.net.URI;
import java.util.Collections;
import net.evendanan.bazel.mvn.api.Dependency;
import org.junit.Assert;
import org.junit.Test;

public class DependencyTest {
    public static Dependency create(final String groupId, final String artifactId, final String version) {
        return new Dependency(groupId, artifactId, version, "jar",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                URI.create("https://maven.com/maven2/"
                        + groupId.replace('.', '/') + "/"
                        + artifactId.replace('.', '/') + "/"
                        + version + "/lib.jar"));
    }

    @Test
    public void testConstructor() {
        Dependency dependency = new Dependency("net.group", "some_lib", "1.2", "jar",
                Collections.singletonList(create("net.group", "dep1", "1.3")),
                Collections.singletonList(create("net.group", "export1", "1.4")),
                Collections.singletonList(create("net.group", "runtime", "1.5")),
                URI.create("https://maven.com/m2/com/example/lib.jar"));

        Assert.assertEquals("net.group", dependency.groupId());
        Assert.assertEquals("some_lib", dependency.artifactId());
        Assert.assertEquals("1.2", dependency.version());
        Assert.assertEquals("jar", dependency.packaging());

        Assert.assertEquals(1, dependency.dependencies().size());
        Assert.assertEquals("1.3", dependency.dependencies().iterator().next().version());

        Assert.assertEquals(1, dependency.exports().size());
        Assert.assertEquals("1.4", dependency.exports().iterator().next().version());

        Assert.assertEquals(1, dependency.runtimeDependencies().size());
        Assert.assertEquals("1.5", dependency.runtimeDependencies().iterator().next().version());
    }

    @Test
    public void testMavenFunctions() {
        Dependency dependency = new Dependency("net.group", "some_lib", "1.2", "jar",
                Collections.singletonList(create("net.group", "dep1", "1.3")),
                Collections.singletonList(create("net.group", "export1", "1.4")),
                Collections.singletonList(create("net.group", "runtime", "1.5")),
                URI.create("https://maven.com/m2/com/example/lib.jar"));

        Assert.assertEquals("net.group:some_lib:1.2", dependency.mavenCoordinates());
        Assert.assertEquals("net_group__some_lib__1_2", dependency.repositoryRuleName());
        Assert.assertEquals("net_group__some_lib", dependency.targetName());
    }
}