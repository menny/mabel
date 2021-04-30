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
    private DependencyTools mUnderTest;

    public static Dependency create(
            final String groupId, final String artifactId, final String version) {
        final URI urlBase =
                URI.create(
                        "https://maven.com/maven2/"
                                + groupId.replace('.', '/')
                                + "/"
                                + artifactId.replace('.', '/')
                                + "/"
                                + version
                                + "/");
        return Dependency.builder()
                .mavenCoordinate(MavenCoordinate.create(groupId, artifactId, version, "jar"))
                .url(urlBase.resolve("lib.jar").toASCIIString())
                .sourcesUrl(urlBase.resolve("lib-sources.jar").toASCIIString())
                .javadocUrl(urlBase.resolve("lib-javadoc.jar").toASCIIString())
                .build();
    }

    @Before
    public void setup() {
        mUnderTest = new DependencyTools();
    }

    @Test
    public void testMavenFunctions() {
        Dependency dependency =
                Dependency.builder()
                        .mavenCoordinate(
                                MavenCoordinate.create("net.group", "some_lib", "1.2", "jar"))
                        .build();

        Assert.assertEquals("net.group:some_lib:1.2", mUnderTest.mavenCoordinates(dependency));
        Assert.assertEquals("net_group__some_lib__1_2", mUnderTest.repositoryRuleName(dependency));
        Assert.assertEquals("net_group__some_lib", mUnderTest.targetName(dependency));
    }

    @Test
    public void testNameEscaping() {
        assertNames(
                create("net.evendanan", "lib1", "1.2.0"),
                "net.evendanan:lib1:1.2.0",
                "net_evendanan__lib1",
                "net_evendanan__lib1__1_2_0");
        assertNames(
                create("net.even-danan", "lib-1", "1.2.0-SNAPSHOT"),
                "net.even-danan:lib-1:1.2.0-SNAPSHOT",
                "net_even_danan__lib_1",
                "net_even_danan__lib_1__1_2_0_SNAPSHOT");
        assertNames(
                create("com.google.errorprone", "javac", "9+181.r4173.1"),
                "com.google.errorprone:javac:9+181.r4173.1",
                "com_google_errorprone__javac",
                "com_google_errorprone__javac__9_181_r4173_1");
    }

    private void assertNames(
            Dependency dependency, String mavenName, String targetName, String repositoryRuleName) {
        Assert.assertEquals(mavenName, mUnderTest.mavenCoordinates(dependency));
        Assert.assertEquals(targetName, mUnderTest.targetName(dependency));
        Assert.assertEquals(repositoryRuleName, mUnderTest.repositoryRuleName(dependency));
    }

    @Test
    public void testLicenseTypeParsing() {
        Assert.assertEquals("UNKNOWN", LicenseTools.typeFromLicenseName(""));
        Assert.assertEquals("UNKNOWN", LicenseTools.typeFromLicenseName(null));
        Assert.assertEquals("UNKNOWN", LicenseTools.typeFromLicenseName("some-unknown-license"));

        Assert.assertEquals("Apache", LicenseTools.typeFromLicenseName("Apache 2.0"));
        Assert.assertEquals("Apache", LicenseTools.typeFromLicenseName("Apache 2"));
        Assert.assertEquals("Apache", LicenseTools.typeFromLicenseName("APACHE-2"));
        Assert.assertEquals("Apache", LicenseTools.typeFromLicenseName("Apache License"));
        Assert.assertEquals("Apache", LicenseTools.typeFromLicenseName("Similar to Apache License but with the acknowledgment clause removed"));
        Assert.assertEquals("Apache", LicenseTools.typeFromLicenseName("ASF 2.0"));
        Assert.assertEquals("Apache", LicenseTools.typeFromLicenseName("Apache License, Version 2.0"));
        Assert.assertEquals("Apache", LicenseTools.typeFromLicenseName("The Apache Software License, Version 2.0"));
        Assert.assertEquals("MIT", LicenseTools.typeFromLicenseName("The MIT License"));
        Assert.assertEquals("MIT", LicenseTools.typeFromLicenseName("MIT License"));
        Assert.assertEquals("MIT", LicenseTools.typeFromLicenseName("MIT"));
        Assert.assertEquals("Bouncy Castle", LicenseTools.typeFromLicenseName("Bouncy Castle Licence"));
        Assert.assertEquals("BSD", LicenseTools.typeFromLicenseName("New BSD License"));
        Assert.assertEquals("BSD", LicenseTools.typeFromLicenseName("BSD 2-Clause License"));
        Assert.assertEquals("BSD", LicenseTools.typeFromLicenseName("BSD License"));
        Assert.assertEquals("Facebook", LicenseTools.typeFromLicenseName("Facebook Platform License"));
        Assert.assertEquals("JSON", LicenseTools.typeFromLicenseName("The JSON License"));
        Assert.assertEquals("Common-Public", LicenseTools.typeFromLicenseName("Common Public License Version 1.0"));
        Assert.assertEquals("CDDL", LicenseTools.typeFromLicenseName("CDDL + GPLv2 with classpath exception"));
        Assert.assertEquals("CDDL", LicenseTools.typeFromLicenseName("CDDL/GPLv2+CE"));
        Assert.assertEquals("CDDL", LicenseTools.typeFromLicenseName("COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0"));
        Assert.assertEquals("Google Cloud", LicenseTools.typeFromLicenseName("Google Cloud Software License"));
        Assert.assertEquals("Indiana University", LicenseTools.typeFromLicenseName("Indiana University Extreme! Lab Software License, version 1.1.1"));
        Assert.assertEquals("Indiana University", LicenseTools.typeFromLicenseName("Indiana University Extreme! Lab Software License, version 1.2"));
        Assert.assertEquals("Indiana University", LicenseTools.typeFromLicenseName("Indiana University Extreme! Lab Software License"));
        Assert.assertEquals("ICU", LicenseTools.typeFromLicenseName("ICU License"));

        Assert.assertEquals("Eclipse", LicenseTools.typeFromLicenseName("Eclipse Public License 1.0"));
        Assert.assertEquals("Eclipse", LicenseTools.typeFromLicenseName("Eclipse Public License v2.0"));
        Assert.assertEquals("Eclipse", LicenseTools.typeFromLicenseName("Eclipse Public License 2.0"));
        Assert.assertEquals("Eclipse", LicenseTools.typeFromLicenseName("Eclipse Public License v 1.0"));
        Assert.assertEquals("Eclipse",LicenseTools.typeFromLicenseName("Eclipse Public License, Version 1.0"));
        Assert.assertEquals("Eclipse", LicenseTools.typeFromLicenseName("EPL 1"));
        Assert.assertEquals("Mozilla", LicenseTools.typeFromLicenseName("MPL 1.1"));
        Assert.assertEquals("Mozilla", LicenseTools.typeFromLicenseName("Mozilla License"));

        Assert.assertEquals("GPL", LicenseTools.typeFromLicenseName("GNU GPL v2"));
        Assert.assertEquals("GPL", LicenseTools.typeFromLicenseName("GPL 3.0"));
        Assert.assertEquals("GPL",LicenseTools.typeFromLicenseName("GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1"));
        Assert.assertEquals("GPL", LicenseTools.typeFromLicenseName("GNU Lesser Public License"));
        Assert.assertEquals("GPL", LicenseTools.typeFromLicenseName("LGPL"));

        Assert.assertEquals("CC0", LicenseTools.typeFromLicenseName("CC0 1.0 Universal License"));
        Assert.assertEquals("Public-Domain", LicenseTools.typeFromLicenseName("Public Domain"));
        Assert.assertEquals("AOSP", LicenseTools.typeFromLicenseName("Android Software Development Kit License"));
        Assert.assertEquals("NO-WARRANTY", LicenseTools.typeFromLicenseName("provided without support or warranty"));

        Assert.assertEquals("WTFPL", LicenseTools.typeFromLicenseName("WTFPL"));
    }

    @Test
    public void testLicenseClassParsing() {
        Assert.assertNull(LicenseTools.classFromLicenseName(""));
        Assert.assertNull(LicenseTools.classFromLicenseName(null));
        Assert.assertNull(LicenseTools.classFromLicenseName("some-unknown-license"));

        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("Apache 2.0"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("Apache 2"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("APACHE-2"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("Apache License"));
        Assert.assertEquals(
                License.Class.notice,
                LicenseTools.classFromLicenseName(
                        "Similar to Apache License but with the acknowledgment clause removed"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("ASF 2.0"));
        Assert.assertEquals(
                License.Class.notice, LicenseTools.classFromLicenseName("Apache License, Version 2.0"));
        Assert.assertEquals(
                License.Class.notice,
                LicenseTools.classFromLicenseName("The Apache Software License, Version 2.0"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("The MIT License"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("MIT License"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("MIT"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("Bouncy Castle Licence"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("New BSD License"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("BSD 2-Clause License"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("BSD License"));
        Assert.assertEquals(
                License.Class.notice, LicenseTools.classFromLicenseName("Facebook Platform License"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("The JSON License"));
        Assert.assertEquals(
                License.Class.notice, LicenseTools.classFromLicenseName("Common Public License Version 1.0"));
        Assert.assertEquals(
                License.Class.notice,
                LicenseTools.classFromLicenseName("CDDL + GPLv2 with classpath exception"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("CDDL/GPLv2+CE"));
        Assert.assertEquals(
                License.Class.notice,
                LicenseTools.classFromLicenseName(
                        "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0"));
        Assert.assertEquals(
                License.Class.notice, LicenseTools.classFromLicenseName("Google Cloud Software License"));
        Assert.assertEquals(
                License.Class.notice,
                LicenseTools.classFromLicenseName(
                        "Indiana University Extreme! Lab Software License, version 1.1.1"));
        Assert.assertEquals(
                License.Class.notice,
                LicenseTools.classFromLicenseName(
                        "Indiana University Extreme! Lab Software License, version 1.2"));
        Assert.assertEquals(
                License.Class.notice,
                LicenseTools.classFromLicenseName("Indiana University Extreme! Lab Software License"));
        Assert.assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("ICU License"));

        Assert.assertEquals(
                License.Class.reciprocal, LicenseTools.classFromLicenseName("Eclipse Public License 1.0"));
        Assert.assertEquals(
                License.Class.reciprocal, LicenseTools.classFromLicenseName("Eclipse Public License v2.0"));
        Assert.assertEquals(
                License.Class.reciprocal, LicenseTools.classFromLicenseName("Eclipse Public License 2.0"));
        Assert.assertEquals(
                License.Class.reciprocal, LicenseTools.classFromLicenseName("Eclipse Public License v 1.0"));
        Assert.assertEquals(
                License.Class.reciprocal,
                LicenseTools.classFromLicenseName("Eclipse Public License, Version 1.0"));
        Assert.assertEquals(License.Class.reciprocal, LicenseTools.classFromLicenseName("EPL 1"));
        Assert.assertEquals(License.Class.reciprocal, LicenseTools.classFromLicenseName("MPL 1.1"));
        Assert.assertEquals(License.Class.reciprocal, LicenseTools.classFromLicenseName("Mozilla License"));

        Assert.assertEquals(License.Class.restricted, LicenseTools.classFromLicenseName("GNU GPL v2"));
        Assert.assertEquals(License.Class.restricted, LicenseTools.classFromLicenseName("GPL 3.0"));
        Assert.assertEquals(
                License.Class.restricted,
                LicenseTools.classFromLicenseName("GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1"));
        Assert.assertEquals(
                License.Class.restricted, LicenseTools.classFromLicenseName("GNU Lesser Public License"));
        Assert.assertEquals(License.Class.restricted, LicenseTools.classFromLicenseName("LGPL"));

        Assert.assertEquals(
                License.Class.unencumbered, LicenseTools.classFromLicenseName("CC0 1.0 Universal License"));
        Assert.assertEquals(License.Class.unencumbered, LicenseTools.classFromLicenseName("Public Domain"));
        Assert.assertEquals(
                License.Class.unencumbered,
                LicenseTools.classFromLicenseName("Android Software Development Kit License"));
        Assert.assertEquals(
                License.Class.unencumbered,
                LicenseTools.classFromLicenseName("provided without support or warranty"));

        Assert.assertEquals(License.Class.permissive, LicenseTools.classFromLicenseName("WTFPL"));
    }

    @Test
    public void testAutoValueLicenseCreate() {
        Assert.assertEquals("License", License.create("License", "https://example.com/license.txt").name());
        Assert.assertEquals("https://example.com/license.txt", License.create("License", "https://example.com/license.txt").url());


        Assert.assertEquals("", License.create("", "whatever").name());
        Assert.assertEquals("", License.create(null, "whatever").name());

        Assert.assertEquals("", License.create("License", "").url());
        Assert.assertEquals("", License.create("License", null).url());

        Assert.assertEquals("", License.create(null, null).name());
    }
}
