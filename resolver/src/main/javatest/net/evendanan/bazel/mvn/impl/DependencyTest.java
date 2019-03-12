package net.evendanan.bazel.mvn.impl;

import java.net.URI;
import java.util.Collections;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.License;
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
                Collections.singleton(License.notice));

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
        Assert.assertEquals(License.notice, dependency.licenses().iterator().next());
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
    public void testNameEscaping() {
        assertNames(create("net.evendanan", "lib1", "1.2.0"),
                "net.evendanan:lib1:1.2.0", "net_evendanan__lib1", "net_evendanan__lib1__1_2_0");
        assertNames(create("net.even-danan", "lib-1", "1.2.0-SNAPSHOT"),
                "net.even-danan:lib-1:1.2.0-SNAPSHOT", "net_even_danan__lib_1", "net_even_danan__lib_1__1_2_0_SNAPSHOT");
        assertNames(create("com.google.errorprone", "javac", "9+181.r4173.1"),
                "com.google.errorprone:javac:9+181.r4173.1", "com_google_errorprone__javac", "com_google_errorprone__javac__9_181_r4173_1");

    }

    private void assertNames(Dependency dependency, String mavenName, String targetName, String repositoryRuleName) {
        Assert.assertEquals(mavenName, dependency.mavenCoordinates());
        Assert.assertEquals(targetName, dependency.targetName());
        Assert.assertEquals(repositoryRuleName, dependency.repositoryRuleName());
    }

    @Test
    public void testLicenseParsing() {
        Assert.assertNull(License.fromLicenseName(""));
        Assert.assertNull(License.fromLicenseName(null));

        Assert.assertEquals(License.notice, License.fromLicenseName("Apache 2.0"));
        Assert.assertEquals(License.notice, License.fromLicenseName("Apache License"));
        Assert.assertEquals(License.notice, License.fromLicenseName("Similar to Apache License but with the acknowledgment clause removed"));
        Assert.assertEquals(License.notice, License.fromLicenseName("ASF 2.0"));
        Assert.assertEquals(License.notice, License.fromLicenseName("Apache License, Version 2.0"));
        Assert.assertEquals(License.notice, License.fromLicenseName("The Apache Software License, Version 2.0"));
        Assert.assertEquals(License.notice, License.fromLicenseName("The MIT License"));
        Assert.assertEquals(License.notice, License.fromLicenseName("MIT License"));
        Assert.assertEquals(License.notice, License.fromLicenseName("MIT"));
        Assert.assertEquals(License.notice, License.fromLicenseName("Bouncy Castle Licence"));
        Assert.assertEquals(License.notice, License.fromLicenseName("New BSD License"));
        Assert.assertEquals(License.notice, License.fromLicenseName("BSD 2-Clause License"));
        Assert.assertEquals(License.notice, License.fromLicenseName("BSD License"));
        Assert.assertEquals(License.notice, License.fromLicenseName("Facebook Platform License"));
        Assert.assertEquals(License.notice, License.fromLicenseName("The JSON License"));
        Assert.assertEquals(License.notice, License.fromLicenseName("Common Public License Version 1.0"));
        Assert.assertEquals(License.notice, License.fromLicenseName("CDDL + GPLv2 with classpath exception"));
        Assert.assertEquals(License.notice, License.fromLicenseName("CDDL/GPLv2+CE"));
        Assert.assertEquals(License.notice, License.fromLicenseName("COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0"));
        Assert.assertEquals(License.notice, License.fromLicenseName("Google Cloud Software License"));
        Assert.assertEquals(License.notice, License.fromLicenseName("Indiana University Extreme! Lab Software License, version 1.1.1"));
        Assert.assertEquals(License.notice, License.fromLicenseName("Indiana University Extreme! Lab Software License, version 1.2"));
        Assert.assertEquals(License.notice, License.fromLicenseName("Indiana University Extreme! Lab Software License"));
        Assert.assertEquals(License.notice, License.fromLicenseName("ICU License"));

        Assert.assertEquals(License.reciprocal, License.fromLicenseName("Eclipse Public License 1.0"));
        Assert.assertEquals(License.reciprocal, License.fromLicenseName("Eclipse Public License v 1.0"));
        Assert.assertEquals(License.reciprocal, License.fromLicenseName("Eclipse Public License, Version 1.0"));
        Assert.assertEquals(License.reciprocal, License.fromLicenseName("EPL 1"));
        Assert.assertEquals(License.reciprocal, License.fromLicenseName("MPL 1.1"));
        Assert.assertEquals(License.reciprocal, License.fromLicenseName("Mozilla License"));

        Assert.assertEquals(License.restricted, License.fromLicenseName("GNU GPL v2"));
        Assert.assertEquals(License.restricted, License.fromLicenseName("GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1"));
        Assert.assertEquals(License.restricted, License.fromLicenseName("GNU Lesser Public License"));
        Assert.assertEquals(License.restricted, License.fromLicenseName("LGPL"));

        Assert.assertEquals(License.unencumbered, License.fromLicenseName("CC0 1.0 Universal License"));
        Assert.assertEquals(License.unencumbered, License.fromLicenseName("Public Domain"));
        Assert.assertEquals(License.unencumbered, License.fromLicenseName("Android Software Development Kit License"));
        Assert.assertEquals(License.unencumbered, License.fromLicenseName("provided without support or warranty"));

        Assert.assertEquals(License.permissive, License.fromLicenseName("WTFPL"));
    }
}