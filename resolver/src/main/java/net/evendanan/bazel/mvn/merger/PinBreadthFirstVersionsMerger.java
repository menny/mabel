package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.GraphMerger;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class PinBreadthFirstVersionsMerger implements GraphMerger {
    private static String dependencyKey(Dependency dependency) {
        return String.format(Locale.ROOT, "%s:%s", dependency.getGroupId(), dependency.getArtifactId());
    }

    private static Collection<Dependency> replaceWithPinned(Collection<Dependency> dependencies, MemoizeDependency memoizeDependency) {
        return dependencies.stream()
                .map(memoizeDependency::map)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Dependency> mergeGraphs(final Collection<Dependency> dependencies) {
        Map<String, Dependency> pinnedVersions = new HashMap<>();

        GraphUtils.BfsTraveller(dependencies, (dependency, level) -> pinnedVersions.compute(dependencyKey(dependency),
                (key, previousDep) -> {
                    if (previousDep == null || previousDep.getUrl().equals("")) return dependency;
                    else return previousDep;
                }
        ));

        return replaceWithPinned(dependencies, new MemoizeDependency(pinnedVersions));
    }

    private static class MemoizeDependency extends GraphMemoizator<Dependency> {

        final Map<String, Dependency> pinnedVersions;

        private MemoizeDependency(final Map<String, Dependency> pinnedVersions) {
            this.pinnedVersions = pinnedVersions;
        }

        @Nonnull
        @Override
        protected Dependency calculate(@Nonnull final Dependency original) {
            Dependency pinned = pinnedVersions.get(dependencyKey(original));

            return Dependency.newBuilder(pinned)
                    .clearDependencies().addAllDependencies(replaceWithPinned(pinned.getDependenciesList(), this))
                    .clearExports().addAllExports(replaceWithPinned(pinned.getExportsList(), this))
                    .clearRuntimeDependencies().addAllRuntimeDependencies(replaceWithPinned(pinned.getRuntimeDependenciesList(), this))
                    .build();
        }

        @Override
        protected String getKeyForObject(final Dependency dependency) {
            return dependencyKey(dependency);
        }
    }
}
