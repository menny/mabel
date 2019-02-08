package net.evendanan.bazel.mvn.impl;

import java.net.URI;
import java.util.Collections;
import net.evendanan.bazel.mvn.api.Dependency;
import org.junit.Assert;
import org.junit.Test;

public class DependencyTest {
    public static Dependency create(final String groupId, final String artifactId, final String version) {
        final URI urlBase = URI.create("https://maven.com/maven2/"
                + groupId.replace('.', '/') + "/"
                + artifactId.replace('.', '/') + "/"
                + version + "/");
        return new Dependency(groupId, artifactId, version, "jar",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                urlBase.resolve("lib.jar"),
                urlBase.resolve("lib-sources.jar"),
                urlBase.resolve("lib-javadoc.jar"),
                Collections.emptyList());
    }

    @Test
    public void testConstructor() {
        Dependency dependency = new Dependency("net.group", "some_lib", "1.2", "jar",
                Collections.singletonList(create("net.group", "dep1", "1.3")),
                Collections.singletonList(create("net.group", "export1", "1.4")),
                Collections.singletonList(create("net.group", "runtime", "1.5")),
                URI.create("https://maven.com/m2/com/example/lib.jar"),
                URI.create("https://maven.com/m2/com/example/lib-sources.jar"),
                URI.create("https://maven.com/m2/com/example/lib-javadoc.jar"),
                Collections.singleton(Dependency.License.notice));

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

        Assert.assertEquals("https://maven.com/m2/com/example/lib.jar", dependency.url().toASCIIString());
        Assert.assertEquals("https://maven.com/m2/com/example/lib-sources.jar", dependency.sourcesUrl().toASCIIString());
        Assert.assertEquals("https://maven.com/m2/com/example/lib-javadoc.jar", dependency.javadocUrl().toASCIIString());

        Assert.assertEquals(1, dependency.licenses().size());
        Assert.assertEquals(Dependency.License.notice, dependency.licenses().iterator().next());
    }

    @Test
    public void testMavenFunctions() {
        Dependency dependency = new Dependency("net.group", "some_lib", "1.2", "jar",
                Collections.singletonList(create("net.group", "dep1", "1.3")),
                Collections.singletonList(create("net.group", "export1", "1.4")),
                Collections.singletonList(create("net.group", "runtime", "1.5")),
                URI.create("https://maven.com/m2/com/example/lib.jar"),
                URI.create("https://maven.com/m2/com/example/lib-sources.jar"),
                URI.create("https://maven.com/m2/com/example/lib-javadoc.jar"),
                Collections.emptyList());

        Assert.assertEquals("net.group:some_lib:1.2", dependency.mavenCoordinates());
        Assert.assertEquals("net_group__some_lib__1_2", dependency.repositoryRuleName());
        Assert.assertEquals("net_group__some_lib", dependency.targetName());
    }

    @Test
    public void testLicenseParsing() {
        Assert.assertNull(Dependency.License.fromLicenseName(""));
        Assert.assertNull(Dependency.License.fromLicenseName(null));

        Assert.assertEquals(Dependency.License.notice, Dependency.License.fromLicenseName("Apache 2.0"));
        Assert.assertEquals(Dependency.License.notice, Dependency.License.fromLicenseName("Apache License, Version 2.0"));
        Assert.assertEquals(Dependency.License.notice, Dependency.License.fromLicenseName("The Apache Software License, Version 2.0"));
        Assert.assertEquals(Dependency.License.notice, Dependency.License.fromLicenseName("The MIT License"));
        Assert.assertEquals(Dependency.License.notice, Dependency.License.fromLicenseName("MIT License"));
        Assert.assertEquals(Dependency.License.notice, Dependency.License.fromLicenseName("MIT"));
        Assert.assertEquals(Dependency.License.notice, Dependency.License.fromLicenseName("New BSD License"));

        Assert.assertEquals(Dependency.License.reciprocal, Dependency.License.fromLicenseName("Eclipse Public License 1.0"));
        Assert.assertEquals(Dependency.License.reciprocal, Dependency.License.fromLicenseName("Eclipse Public License, Version 1.0"));

        Assert.assertEquals(Dependency.License.unencumbered, Dependency.License.fromLicenseName("CC0 1.0 Universal License"));
    }
}