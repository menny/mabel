package net.evendanan.bazel.mvn.merger;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

public abstract class GraphMemoizator<T> {
  private final Map<String, T> cache = new HashMap<>();

  @Nonnull
  protected abstract T calculate(@Nonnull T original);

  @Nonnull
  public T map(@Nonnull T original) {
    final String key = getKeyForObject(original);
    if (cache.containsKey(key)) {
      return cache.get(key);
    } else {
      final T revised = calculate(original);
      cache.put(key, revised);
      return revised;
    }
  }

  protected abstract String getKeyForObject(final T object);
}
