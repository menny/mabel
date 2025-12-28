package net.evendanan.bazel.mvn.merger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

public abstract class GraphMemoizator<T> {
  private final Map<String, T> cache = new ConcurrentHashMap<>();

  @Nonnull
  protected abstract T calculate(@Nonnull T original);

  @Nonnull
  public T map(@Nonnull T original) {
    final String key = getKeyForObject(original);
    // ConcurrentHashMap.computeIfAbsent is atomic
    return cache.computeIfAbsent(key, k -> calculate(original));
  }

  protected abstract String getKeyForObject(final T object);
}
