package net.evendanan.bazel.mvn.merger;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.model.Dependency;

public class ClearSrcJarAttribute {
  public static Collection<Dependency> clearSrcJar(final Collection<Dependency> dependencies) {
    return clearSrcJar(dependencies, new MemoizeDependency());
  }

  private static Collection<Dependency> clearSrcJar(
      final Collection<Dependency> dependencies, MemoizeDependency memoizeDependency) {
    return dependencies.stream().map(memoizeDependency::map).collect(Collectors.toList());
  }

  private static class MemoizeDependency extends GraphMemoizator<Dependency> {

    @Nonnull
    @Override
    protected Dependency calculate(@Nonnull final Dependency original) {
      return Dependency.builder(original).sourcesUrl("").build();
    }

    @Override
    protected String getKeyForObject(final Dependency dependency) {
      return DependencyTools.DEFAULT.mavenCoordinates(dependency);
    }
  }
}
