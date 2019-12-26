package net.evendanan.bazel.mvn;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.evendanan.bazel.mvn.api.Dependency;

public class TestUtils {

    public static Dependency createDependency(String mavenDep, String url, String sourcesUrl, List<String> depsLabels, List<String> exportsLabels, List<String> runtimeLabels) {
        final String[] depsPart = mavenDep.split(":", -1);

        return Dependency.newBuilder()
                .setGroupId(depsPart[0])
                .setArtifactId(depsPart[1])
                .setVersion(depsPart.length > 2 ? depsPart[2] : "")
                .setPackaging(url.substring(url.length() - 3))
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


    private static Collection<Dependency> generateDeps(final List<String> depsLabels) {
        return depsLabels.stream()
                .map(label -> Dependency.newBuilder()
                        .setGroupId("safe_mvn")
                        .setArtifactId(label)
                        .build())
                .collect(Collectors.toList());
    }
}
