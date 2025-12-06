package net.evendanan.bazel.mvn.impl;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Function;
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import org.junit.Assert;
import org.junit.Test;

public class WritersTests {

  @Test
  public void testHttpTargetsBuilderWithSha() throws Exception {
    final Function<Dependency, URI> dependencyURIFunction =
        dep -> {
          try {
            final File file = File.createTempFile("testRepositoryRulesMacroWriterWithSha", "test");
            Files.write(file.toPath(), "whatever".getBytes(), StandardOpenOption.CREATE);
            return file.toURI();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        };
    final TargetsBuilders.HttpTargetsBuilder httpTargetsBuilder =
        new TargetsBuilders.HttpTargetsBuilder(true, dependencyURIFunction);

    final List<Target> targets =
        httpTargetsBuilder.buildTargets(
            Dependency.builder()
                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "dep1", "1.2.3", "jar"))
                .url("https://maven.central.org/repo/net/evendanan/dep1/dep1-1.2.3.jar")
                .build(),
            DependencyTools.DEFAULT);

    Assert.assertEquals(1, targets.size());
    Assert.assertTrue(
        targets
            .get(0)
            .outputString("")
            .contains(
                "sha256 = \"85738f8f9a7f1b04b5329c590ebcb9e425925c6d0984089c43a022de4f19c281\""));
  }

  @Test
  public void testHttpTargetsBuilderWithShaButDoesNotGeneratesIfVersionIsSnapshot()
      throws Exception {
    final Function<Dependency, URI> dependencyURIFunction =
        dep -> {
          try {
            final File file =
                File.createTempFile(
                    "testRepositoryRulesMacroWriterWithShaButDoesNotGeneratesIfVersionIsSnapshot",
                    "test");
            Files.write(file.toPath(), "whatever".getBytes(), StandardOpenOption.CREATE);
            return file.toURI();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        };
    final TargetsBuilders.HttpTargetsBuilder httpTargetsBuilder =
        new TargetsBuilders.HttpTargetsBuilder(true, dependencyURIFunction);

    final List<Target> targets =
        httpTargetsBuilder.buildTargets(
            Dependency.builder()
                .mavenCoordinate(
                    MavenCoordinate.create("net.evendanan", "dep1", "1.2.3-SNAPSHOT", "jar"))
                .url("https://maven.central.org/repo/net/evendanan/dep1/dep1-1.2.3-SNAPSHOT.jar")
                .build(),
            DependencyTools.DEFAULT);

    Assert.assertEquals(1, targets.size());
    Assert.assertFalse(targets.get(0).outputString("").contains("sha256"));
  }
}
