package com.google.devtools.bazel.workspace.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactBuilderTest {

  private static Dependency createDepWithVersion(String version) {
    final Dependency dependency = new Dependency();
    dependency.setVersion(version);
    return dependency;
  }

  @Test
  public void testNoProfilesNoPlaceholders() {
    Assert.assertEquals(
        "something",
        ArtifactBuilder.ProfilePlaceholderUtil.replacePlaceholders(
            new Model(), createDepWithVersion("something")));
  }

  @Test
  public void testDoesNotReplaceIfNoPlaceholders() {
    Model model = new Model();
    model.getProfiles().add(new Profile());
    model.getProfiles().get(0).getProperties().put("key", "value");
    model.getProfiles().get(0).getProperties().put("something", "other");

    Assert.assertEquals(
        "something",
        ArtifactBuilder.ProfilePlaceholderUtil.replacePlaceholders(
            model, createDepWithVersion("something")));
  }

  @Test
  public void testDoesNotReplaceIfPlaceholdersWereNotFound() {
    Model model = new Model();
    model.getProfiles().add(new Profile());
    model.getProfiles().get(0).getProperties().put("key", "value");
    model.getProfiles().get(0).getProperties().put("something", "other");

    Assert.assertEquals(
        "${not.found}",
        ArtifactBuilder.ProfilePlaceholderUtil.replacePlaceholders(
            model, createDepWithVersion("${not.found}")));
  }

  @Test
  public void testReplacesFromMultipleProfiles() {
    Model model = new Model();
    model.getProfiles().add(new Profile());
    model.getProfiles().get(0).getProperties().put("key", "value");
    model.getProfiles().get(0).getProperties().put("something", "car");

    model.getProfiles().add(new Profile());
    model.getProfiles().get(1).getProperties().put("key2", "vehicle");

    Assert.assertEquals(
        "car is vehicle",
        ArtifactBuilder.ProfilePlaceholderUtil.replacePlaceholders(
            model, createDepWithVersion("${something} is ${key2}")));
  }

  @Test
  public void testReplacesKeyWithFirstSeenValue() {
    Model model = new Model();
    model.getProfiles().add(new Profile());
    model.getProfiles().get(0).getProperties().put("key", "object");
    model.getProfiles().get(0).getProperties().put("something", "car");

    model.getProfiles().add(new Profile());
    model.getProfiles().get(1).getProperties().put("key", "vehicle");

    Assert.assertEquals(
        "car is object",
        ArtifactBuilder.ProfilePlaceholderUtil.replacePlaceholders(
            model, createDepWithVersion("${something} is ${key}")));
  }

  @Test
  public void testFailsWhenVersionIsNull() {
    NullPointerException caught = null;
    try {
      ArtifactBuilder.ProfilePlaceholderUtil.replacePlaceholders(
          new Model(), createDepWithVersion(null));
    } catch (NullPointerException e) {
      caught = e;
    }

    Assert.assertNotNull(caught);
    Assert.assertTrue(caught.getMessage().endsWith("has no version!"));
  }
}
