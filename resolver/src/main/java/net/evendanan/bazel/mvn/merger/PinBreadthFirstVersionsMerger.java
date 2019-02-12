package net.evendanan.bazel.mvn.merger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.GraphMerger;

public class PinBreadthFirstVersionsMerger implements GraphMerger {
    private static String dependencyKey(Dependency dependency) {
        return String.format(Locale.US, "%s:%s", dependency.groupId(), dependency.artifactId());
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
                    if (previousDep==null || previousDep.url().toASCIIString().equals("")) return dependency;
                    else return previousDep;
                }
        ));

        return replaceWithPinned(dependencies, new MemoizeDependency(pinnedVersions));
    }

    private static class MemoizeDependency extends GraphMemoizator {

        final Map<String, Dependency> pinnedVersions;

        private MemoizeDependency(final Map<String, Dependency> pinnedVersions) {
            this.pinnedVersions = pinnedVersions;
        }

        @Nonnull
        @Override
        protected Dependency calculate(@Nonnull final Dependency original) {
            Dependency pinned = pinnedVersions.get(dependencyKey(original));
            return new Dependency(pinned.groupId(), pinned.artifactId(), pinned.version(), pinned.packaging(),
                    replaceWithPinned(pinned.dependencies(), this),
                    replaceWithPinned(pinned.exports(), this),
                    replaceWithPinned(pinned.runtimeDependencies(), this),
                    pinned.url(), pinned.sourcesUrl(), pinned.javadocUrl(), pinned.licenses());
        }

        @Override
        protected String getKeyForDependency(final Dependency dependency) {
            return dependencyKey(dependency);
        }
    }
}
