package com.google.devtools.bazel.workspace.maven;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactBuilderTest {

    @Test
    public void testNoProfilesNoPlaceholders() {
        Assert.assertEquals(
                "something",
                ArtifactBuilder.ProfilePlaceholderUtil.replacePlaceholders(
                        new Model(), "something"));
    }

    @Test
    public void testDoesNotReplaceIfNoPlaceholders() {
        Model model = new Model();
        model.getProfiles().add(new Profile());
        model.getProfiles().get(0).getProperties().put("key", "value");
        model.getProfiles().get(0).getProperties().put("something", "other");

        Assert.assertEquals(
                "something",
                ArtifactBuilder.ProfilePlaceholderUtil.replacePlaceholders(model, "something"));
    }

    @Test
    public void testDoesNotReplaceIfPlaceholdersWereNotFound() {
        Model model = new Model();
        model.getProfiles().add(new Profile());
        model.getProfiles().get(0).getProperties().put("key", "value");
        model.getProfiles().get(0).getProperties().put("something", "other");

        Assert.assertEquals(
                "${not.found}",
                ArtifactBuilder.ProfilePlaceholderUtil.replacePlaceholders(model, "${not.found}"));
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
                        model, "${something} is ${key2}"));
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
                        model, "${something} is ${key}"));
    }
}
