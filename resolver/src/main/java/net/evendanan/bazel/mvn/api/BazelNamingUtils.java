package net.evendanan.bazel.mvn.api;

import com.google.common.base.CharMatcher;

/** Utility for converting Maven names to Bazel-safe names. */
public final class BazelNamingUtils {
  private static final CharMatcher INVALID_BAZEL_CHARS = CharMatcher.anyOf("+.-").precomputed();

  private BazelNamingUtils() {}

  /**
   * Normalizes a Maven identifier (groupId, artifactId, or version) to be safe for use in Bazel
   * workspace or target names.
   *
   * @param name The original Maven identifier.
   * @return The normalized name with invalid characters (+, ., -) replaced by underscores.
   */
  public static String normalize(String name) {
    if (name == null) {
      return "";
    }
    return INVALID_BAZEL_CHARS.replaceFrom(name, '_');
  }
}
