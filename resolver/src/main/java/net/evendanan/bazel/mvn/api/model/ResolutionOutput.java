package net.evendanan.bazel.mvn.api.model;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ResolutionOutput {
  public static ResolutionOutput create(
      TargetType type,
      ExportsGenerationType exportsGenerationType,
      boolean testOnly,
      Resolution resolution) {
    return new AutoValue_ResolutionOutput(type, exportsGenerationType, testOnly, resolution);
  }

  public abstract TargetType targetType();

  public abstract ExportsGenerationType exportsGenerationType();

  public abstract boolean testOnly();

  public abstract Resolution resolution();
}
