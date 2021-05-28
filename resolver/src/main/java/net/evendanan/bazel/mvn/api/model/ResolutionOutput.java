package net.evendanan.bazel.mvn.api.model;

import com.google.auto.value.AutoValue;

import java.util.Collection;

@AutoValue
public abstract class ResolutionOutput {
    public static ResolutionOutput create(
            TargetType type,
            boolean testOnly,
            Resolution resolution) {
        return new AutoValue_ResolutionOutput(type, testOnly, resolution);
    }

    public abstract TargetType targetType();

    public abstract boolean testOnly();

    public abstract Resolution resolution();
}
