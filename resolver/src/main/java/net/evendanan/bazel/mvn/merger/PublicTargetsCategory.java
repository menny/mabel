package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Target;

import java.util.function.Function;

public class PublicTargetsCategory {

    public static Function<Target, Target> create(Type type) {
        return Function.identity();
    }

    public enum Type {
        all,
        //        recursive_exports,
        //        exports,
        //        requested_deps
    }
}
