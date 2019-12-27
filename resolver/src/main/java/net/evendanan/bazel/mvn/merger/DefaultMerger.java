package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.GraphMerger;

import java.util.Collection;

public class DefaultMerger implements GraphMerger {

    @Override
    public Collection<Dependency> mergeGraphs(Collection<Dependency> dependencies) {
        // strategy
        //1. pinning breadth-first versions and resolve versions on all graphs
        System.out.println("Resolving conflicting versions...");
        final Collection<Dependency> mergedDependencies = new PinBreadthFirstVersionsMerger().mergeGraphs(dependencies);
        //2. de-duping
        System.out.println("Removing duplicate dependencies...");
        return FilterDuplicateDependenciesEntries.filterDuplicateDependencies(mergedDependencies);
    }
}
