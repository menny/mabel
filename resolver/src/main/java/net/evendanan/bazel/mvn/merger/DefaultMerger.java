package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.GraphMerger;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.Resolution;

import java.util.Collection;

public class DefaultMerger implements GraphMerger {

    private final GraphMerger mMainMerger;

    public DefaultMerger(GraphMerger merger) {
        mMainMerger = merger;
    }

    @Override
    public Collection<Dependency> mergeGraphs(Collection<Resolution> dependencies) {
        // strategy
        // 1. pinning breadth-first versions and resolve versions on all graphs
        System.out.print(mMainMerger.getClass().getSimpleName() + ": Resolving conflicting versions...");
        final Collection<Dependency> mergedDependencies = mMainMerger.mergeGraphs(dependencies);
        // 2. de-duping
        System.out.print("Removing duplicate dependencies...");
        return FilterDuplicateDependenciesEntries.filterDuplicateDependencies(mergedDependencies);
    }
}
