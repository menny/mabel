package net.evendanan.bazel.mvn.merger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;

public class DependencyTreeFlatter {

    public static Collection<Dependency> flatten(Collection<Dependency> dependencies) {
        Set<String> seen = new HashSet<>();
        List<Dependency> flatten = new ArrayList<>();

        GraphUtils.DfsTraveller(dependencies, (dep, level) -> {
            if (!seen.contains(DependencyTools.DEFAULT.mavenCoordinates(dep))) {
                seen.add(DependencyTools.DEFAULT.mavenCoordinates(dep));
                flatten.add(dep);
            }
        });

        return flatten;
    }
}
