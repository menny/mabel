package com.google.devtools.bazel.workspace.maven;

import static org.junit.Assert.assertEquals;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Test;

public class RuleTest {

  @Test
  public void testNormalizedWorkspaceName() {
    // Group ID normalization
    assertEquals(
        "com_google_guava", Rule.generateFriendlyName("com.google.guava", "guava").split("__")[0]);
    // Artifact ID normalization
    assertEquals(
        "guava_android",
        Rule.generateFriendlyName("com.google.guava", "guava-android").split("__")[1]);
    // Version normalization
    assertEquals("27_0_1_jre", Rule.generateFullName("a", "b", "27.0.1-jre").split("__")[2]);
  }

  @Test
  public void testGetUri() {
    Artifact artifact = new DefaultArtifact("com.google.guava", "guava", "jar", "27.0.1-jre");
    Rule rule = new Rule(artifact);

    // This indirectly tests getUri() through getUrl()
    assertEquals(
        "https://repo1.maven.org/maven2/com/google/guava/guava/27.0.1-jre/guava-27.0.1-jre.jar",
        rule.getUrl());
  }

  @Test
  public void testGenerateFullName() {
    assertEquals("com_google__guava__27_0", Rule.generateFullName("com.google", "guava", "27.0"));
  }
}
