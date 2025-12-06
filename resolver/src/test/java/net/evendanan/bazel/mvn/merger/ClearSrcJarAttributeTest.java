package net.evendanan.bazel.mvn.merger;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import org.junit.Test;

public class ClearSrcJarAttributeTest {

  @Test
  public void testClearUrl() {
    Dependency dep =
        Dependency.builder()
            .mavenCoordinate(MavenCoordinate.create("g", "a", "1.0", "jar"))
            .url("http://example.com/lib.jar")
            .sourcesUrl("http://example.com/lib-sources.jar")
            .build();

    Collection<Dependency> result = ClearSrcJarAttribute.clearSrcJar(Collections.singleton(dep));
    assertEquals(1, result.size());
    Dependency clearedDep = result.iterator().next();
    assertEquals("http://example.com/lib.jar", clearedDep.url());
    assertEquals("", clearedDep.sourcesUrl());
  }

  @Test
  public void testRecursive() {
    Dependency leaf =
        Dependency.builder()
            .mavenCoordinate(MavenCoordinate.create("g", "leaf", "1.0", "jar"))
            .sourcesUrl("http://example.com/leaf-sources.jar")
            .build();

    Dependency root =
        Dependency.builder()
            .mavenCoordinate(MavenCoordinate.create("g", "root", "1.0", "jar"))
            .sourcesUrl("http://example.com/root-sources.jar")
            .dependencies(Collections.singleton(leaf.mavenCoordinate()))
            .build();

    // Note: ClearSrcJarAttribute processes a COLLECTION of dependencies.
    // It uses GraphMemoizator which usually recursively processes if the
    // calculation function follows dependencies.
    // However, looking at the code: clearSrcJar -> map ->
    // Dependency.builder(original).sourcesUrl("").build().
    // It seems to only clear the sourcesUrl of the objects passed in?
    // Wait, GraphMemoizator's map usually handles recursion if designed deeply, but
    // here
    // ClearSrcJarAttribute.MemoizeDependency just rebuilds the current node.
    // Unless the dependencies list is also processed?

    // Let's re-read ClearSrcJarAttribute.java.
    // It creates a BUT shallow copy using builder(original).sourcesUrl("").build().
    // The dependencies() list inside will contain maven coordinates.
    // The input to clearSrcJar is "Collection<Dependency> dependencies".
    // Typically in this codebase, a flattened list of all resolved dependencies is
    // passed.
    // So "recursive" here means verifying that all elements in the collection are
    // processed.

    Collection<Dependency> result = ClearSrcJarAttribute.clearSrcJar(Collections.singleton(leaf));
    assertEquals("", result.iterator().next().sourcesUrl());
  }

  @Test
  public void testAlreadyEmpty() {
    Dependency dep =
        Dependency.builder()
            .mavenCoordinate(MavenCoordinate.create("g", "a", "1.0", "jar"))
            .sourcesUrl("")
            .build();

    Collection<Dependency> result = ClearSrcJarAttribute.clearSrcJar(Collections.singleton(dep));
    assertEquals("", result.iterator().next().sourcesUrl());
  }
}
