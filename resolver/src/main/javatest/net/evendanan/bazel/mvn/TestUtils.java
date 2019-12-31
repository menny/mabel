package net.evendanan.bazel.mvn;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.MavenCoordinate;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TestUtils {

    public static Dependency createDependency(String mavenDep, String url, String sourcesUrl, List<String> depsLabels, List<String> exportsLabels, List<String> runtimeLabels) {
        final String[] depsPart = mavenDep.split(":", -1);

        return Dependency.newBuilder()
                .setMavenCoordinate(MavenCoordinate.newBuilder()
                        .setGroupId(depsPart[0])
                        .setArtifactId(depsPart[1])
                        .setVersion(depsPart.length > 2 ? depsPart[2] : "")
                        .setPackaging(url.substring(url.length() - 3))
                        .build())
                .addAllDependencies(generateDeps(depsLabels))
                .addAllExports(generateDeps(exportsLabels))
                .addAllRuntimeDependencies(generateDeps(runtimeLabels))
                .setUrl(url)
                .setSourcesUrl(sourcesUrl)
                .setJavadocUrl("")
                .build();
    }

    public static Dependency createDependency(String mavenDep, String url, List<String> depsLabels, List<String> exportsLabels, List<String> runtimeLabels) {
        return createDependency(mavenDep, url, "", depsLabels, exportsLabels, runtimeLabels);
    }


    private static Collection<MavenCoordinate> generateDeps(final List<String> depsLabels) {
        return depsLabels.stream()
                .map(label -> MavenCoordinate.newBuilder()
                        .setGroupId("safe_mvn")
                        .setArtifactId(label)
                        .build())
                .collect(Collectors.toList());
    }
}
