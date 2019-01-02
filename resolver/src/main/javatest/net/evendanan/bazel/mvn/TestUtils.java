package net.evendanan.bazel.mvn;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.evendanan.bazel.mvn.api.Dependency;
import org.mockito.Mockito;

public class TestUtils {


    public static Dependency createDependency(String mavenDep, String url, List<String> depsLabels, List<String> exportsLabels, List<String> runtimeLabels) {
        final String[] depsPart = mavenDep.split(":", -1);

        return new Dependency(depsPart[0], depsPart[1], depsPart.length > 2 ? depsPart[2]:"",
                url.substring(url.length() - 3),
                generateDeps(depsLabels), generateDeps(exportsLabels), generateDeps(runtimeLabels),
                URI.create(url),
                URI.create(""),
                URI.create(""),
                Collections.emptyList());
    }


    private static Collection<Dependency> generateDeps(final List<String> depsLabels) {
        Collection<Dependency> deps = new ArrayList<>(depsLabels.size());
        depsLabels.forEach(dep -> {
            final Dependency dependency = Mockito.mock(Dependency.class);
            Mockito.doReturn("safe_mvn__" + dep).when(dependency).targetName();
            deps.add(dependency);
        });
        return deps;
    }
}
