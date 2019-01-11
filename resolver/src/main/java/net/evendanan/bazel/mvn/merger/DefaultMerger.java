package net.evendanan.bazel.mvn.merger;

import java.util.Collection;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.GraphMerger;

public class DefaultMerger implements GraphMerger {

    @Override
    public Collection<Dependency> mergeGraphs(Collection<Dependency> dependencies, final Collection<String> excludes) {
        // strategy
        //0. filter-out all dependencies in the exclude list
        dependencies = ExcludesFilter.filterDependencies(dependencies, excludes);
        //1. pinning breadth-first versions and resolve versions on all graphs
        final Collection<Dependency> mergedDependencies = new PinBreadthFirstVersionsMerger().mergeGraphs(dependencies, excludes);
        //2. de-duping
        final Collection<Dependency> deDuped = new DuplicatesDepsRemovingMerger().mergeGraphs(mergedDependencies, excludes);

        return VerifyNoConflictingVersions.checkNoConflictingVersions(deDuped);
    }

}
