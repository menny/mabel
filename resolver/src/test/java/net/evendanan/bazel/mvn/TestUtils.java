package net.evendanan.bazel.mvn;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;

public class TestUtils {

  public static Dependency createDependency(
      String mavenDep,
      String url,
      String sourcesUrl,
      List<String> depsLabels,
      List<String> exportsLabels,
      List<String> runtimeLabels) {
    final String[] depsPart = mavenDep.split(":", -1);

    return Dependency.builder()
        .mavenCoordinate(
            MavenCoordinate.create(
                depsPart[0],
                depsPart[1],
                depsPart.length > 2 ? depsPart[2] : "",
                url.substring(url.length() - 3)))
        .dependencies(generateDeps(depsLabels))
        .exports(generateDeps(exportsLabels))
        .runtimeDependencies(generateDeps(runtimeLabels))
        .url(url)
        .sourcesUrl(sourcesUrl)
        .javadocUrl("")
        .build();
  }

  public static Dependency markAsTestOnly(Dependency dependency) {
    return Dependency.builder(dependency).testOnly(true).build();
  }

  public static Dependency createDependency(
      String mavenDep,
      String url,
      List<String> depsLabels,
      List<String> exportsLabels,
      List<String> runtimeLabels) {
    return createDependency(mavenDep, url, "", depsLabels, exportsLabels, runtimeLabels);
  }

  private static Collection<MavenCoordinate> generateDeps(final List<String> depsLabels) {
    return depsLabels.stream()
        .map(label -> MavenCoordinate.create("safe_mvn", label, "", ""))
        .collect(Collectors.toList());
  }
}
