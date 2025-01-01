package net.evendanan.bazel.mvn.api.model;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class License {
  public enum Class {
    restricted,
    reciprocal,
    notice,
    permissive,
    unencumbered
  }

  public abstract String name();

  public abstract String url();

  public static License create(String name, String url) {
    return new AutoValue_License(nullToEmpty(name), nullToEmpty(url));
  }

  private static String nullToEmpty(String str) {
    if (str == null) return "";
    else return str;
  }
}
