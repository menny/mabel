package net.evendanan.bazel.mvn.merger;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import net.evendanan.bazel.mvn.api.Dependency;

public abstract class GraphMemoizator {
    private final Map<String, Dependency> cache = new HashMap<>();

    @Nonnull
    protected abstract Dependency calculate(@Nonnull Dependency original);

    @Nonnull
    public Dependency map(@Nonnull Dependency original) {
        final String key = getKeyForDependency(original);
        if (cache.containsKey(key)) {
            return cache.get(key);
        } else {
            final Dependency revisedDependency = calculate(original);
            cache.put(key, revisedDependency);
            return revisedDependency;
        }
    }

    protected abstract String getKeyForDependency(final Dependency dependency);
}
