package net.evendanan.bazel.mvn.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BazelNamingUtilsTest {

  @Test
  public void testNormalize() {
    assertEquals("com_google_guava", BazelNamingUtils.normalize("com.google.guava"));
    assertEquals("guava_android", BazelNamingUtils.normalize("guava-android"));
    assertEquals("27_0_1_jre", BazelNamingUtils.normalize("27.0.1-jre"));
    assertEquals("my_version_1_0_", BazelNamingUtils.normalize("my.version-1.0+"));
    assertEquals("", BazelNamingUtils.normalize(null));
    assertEquals("", BazelNamingUtils.normalize(""));
    assertEquals("no_special_chars", BazelNamingUtils.normalize("no_special_chars"));
  }
}
