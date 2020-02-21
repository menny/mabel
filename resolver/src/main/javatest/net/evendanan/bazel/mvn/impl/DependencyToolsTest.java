package net.evendanan.bazel.mvn.impl;

import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.LicenseTools;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.License;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

public class DependencyToolsTest {
    public static Dependency create(final String groupId, final String artifactId, final String version) {
        final URI urlBase = URI.create("https://maven.com/maven2/"
                + groupId.replace('.', '/') + "/"
                + artifactId.replace('.', '/') + "/"
                + version + "/");
        return Dependency.builder()
                .mavenCoordinate(MavenCoordinate.create(groupId, artifactId, version, "jar"))
                .url(urlBase.resolve("lib.jar").toASCIIString())
                .sourcesUrl(urlBase.resolve("lib-sources.jar").toASCIIString())
                .javadocUrl(urlBase.resolve("lib-javadoc.jar").toASCIIString())
                .build();
    }

    private DependencyTools mUnderTest;

    @Before
    public void setup() {
        mUnderTest = new DependencyTools();
    }

    @Test
    public void testMavenFunctions() {
        Dependency dependency = Dependency.builder()
                .mavenCoordinate(
                        MavenCoordinate.create("net.group", "some_lib", "1.2", "jar"))
                .build();

        Assert.assertEquals("net.group:some_lib:1.2", mUnderTest.mavenCoordinates(dependency));
        Assert.assertEquals("net_group__some_lib__1_2", mUnderTest.repositoryRuleName(dependency));
        Assert.assertEquals("net_group__some_lib", mUnderTest.targetName(dependency));
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
        Assert.assertEquals(mavenName, mUnderTest.mavenCoordinates(dependency));
        Assert.assertEquals(targetName, mUnderTest.targetName(dependency));
        Assert.assertEquals(repositoryRuleName, mUnderTest.repositoryRuleName(dependency));
    }

    @Test
    public void testLicenseParsing() {
        Assert.assertNull(LicenseTools.fromLicenseName(""));
        Assert.assertNull(LicenseTools.fromLicenseName(null));
        Assert.assertNull(LicenseTools.fromLicenseName("some-unknown-license"));

        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("Apache 2.0"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("Apache 2"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("APACHE-2"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("Apache License"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("Similar to Apache License but with the acknowledgment clause removed"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("ASF 2.0"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("Apache License, Version 2.0"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("The Apache Software License, Version 2.0"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("The MIT License"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("MIT License"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("MIT"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("Bouncy Castle Licence"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("New BSD License"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("BSD 2-Clause License"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("BSD License"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("Facebook Platform License"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("The JSON License"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("Common Public License Version 1.0"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("CDDL + GPLv2 with classpath exception"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("CDDL/GPLv2+CE"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("Google Cloud Software License"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("Indiana University Extreme! Lab Software License, version 1.1.1"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("Indiana University Extreme! Lab Software License, version 1.2"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("Indiana University Extreme! Lab Software License"));
        Assert.assertEquals(License.notice, LicenseTools.fromLicenseName("ICU License"));

        Assert.assertEquals(License.reciprocal, LicenseTools.fromLicenseName("Eclipse Public License 1.0"));
        Assert.assertEquals(License.reciprocal, LicenseTools.fromLicenseName("Eclipse Public License v 1.0"));
        Assert.assertEquals(License.reciprocal, LicenseTools.fromLicenseName("Eclipse Public License, Version 1.0"));
        Assert.assertEquals(License.reciprocal, LicenseTools.fromLicenseName("EPL 1"));
        Assert.assertEquals(License.reciprocal, LicenseTools.fromLicenseName("MPL 1.1"));
        Assert.assertEquals(License.reciprocal, LicenseTools.fromLicenseName("Mozilla License"));

        Assert.assertEquals(License.restricted, LicenseTools.fromLicenseName("GNU GPL v2"));
        Assert.assertEquals(License.restricted, LicenseTools.fromLicenseName("GPL 3.0"));
        Assert.assertEquals(License.restricted, LicenseTools.fromLicenseName("GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1"));
        Assert.assertEquals(License.restricted, LicenseTools.fromLicenseName("GNU Lesser Public License"));
        Assert.assertEquals(License.restricted, LicenseTools.fromLicenseName("LGPL"));

        Assert.assertEquals(License.unencumbered, LicenseTools.fromLicenseName("CC0 1.0 Universal License"));
        Assert.assertEquals(License.unencumbered, LicenseTools.fromLicenseName("Public Domain"));
        Assert.assertEquals(License.unencumbered, LicenseTools.fromLicenseName("Android Software Development Kit License"));
        Assert.assertEquals(License.unencumbered, LicenseTools.fromLicenseName("provided without support or warranty"));

        Assert.assertEquals(License.permissive, LicenseTools.fromLicenseName("WTFPL"));
    }
}