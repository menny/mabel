package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.GraphMerger;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.Resolution;

import java.util.Collection;

public class DefaultMerger implements GraphMerger {

    @Override
    public Collection<Dependency> mergeGraphs(Collection<Resolution> dependencies) {
        // strategy
        //1. pinning breadth-first versions and resolve versions on all graphs
        System.out.print("Resolving conflicting versions...");
        final Collection<Dependency> mergedDependencies = new PinBreadthFirstVersionsMerger().mergeGraphs(dependencies);
        //2. de-duping
        System.out.print("Removing duplicate dependencies...");
        return FilterDuplicateDependenciesEntries.filterDuplicateDependencies(mergedDependencies);
    }
}
