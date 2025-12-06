package net.evendanan.bazel.mvn.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import net.evendanan.bazel.mvn.api.model.License;
import org.junit.Test;

public class LicenseToolsTest {

  @Test
  public void testKnownLicenses() {
    assertEquals("Apache", LicenseTools.typeFromLicenseName("Apache License 2.0"));
    assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("Apache License 2.0"));

    assertEquals("MIT", LicenseTools.typeFromLicenseName("The MIT License"));
    assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("The MIT License"));

    assertEquals("Bouncy Castle", LicenseTools.typeFromLicenseName("Bouncy Castle License"));
    assertEquals(License.Class.notice, LicenseTools.classFromLicenseName("Bouncy Castle License"));

    assertEquals("BST", "BSD", LicenseTools.typeFromLicenseName("BSD 3-Clause"));
  }

  @Test
  public void testUnknown() {
    assertEquals("UNKNOWN", LicenseTools.typeFromLicenseName("Some Random Proprietary License"));
    assertNull(LicenseTools.classFromLicenseName("Some Random Proprietary License"));
  }

  @Test
  public void testNullOrEmpty() {
    assertEquals("UNKNOWN", LicenseTools.typeFromLicenseName(null));
    assertNull(LicenseTools.classFromLicenseName(null));

    assertEquals("UNKNOWN", LicenseTools.typeFromLicenseName(""));
    assertNull(LicenseTools.classFromLicenseName(""));
  }

  @Test
  public void testRegexRobustness() {
    // Case insensitivity
    assertEquals("Apache", LicenseTools.typeFromLicenseName("apache license 2.0"));

    // Whitespace (if regex allows) - most regexes in LicenseTools seem to use .*
    // which matches whitespace
    assertEquals("Apache", LicenseTools.typeFromLicenseName("   Apache   2.0   "));
  }
}
