package net.evendanan.bazel.mvn.api.model;

public enum ExportsGenerationType {
    /**
     * USe default exports-generation. This can only be used in `artifact`.
     */
    inherit,

    /**
     * Generate exports for all targets.
     */
    all,

    /**
     * Generate export only for top-level (requested) dependencies.
     */
    requested_deps,


    /**
     * Never generate exports.
     */
    none;

    public static ExportsGenerationType prioritizeType(ExportsGenerationType type1, ExportsGenerationType type2) {
        if (type1.equals(ExportsGenerationType.inherit))
            throw new IllegalArgumentException("type1 is inherit, which is not supported");
        if (type2.equals(ExportsGenerationType.inherit))
            throw new IllegalArgumentException("type2 is inherit, which is not supported");

        final int highest = Math.min(type1.ordinal(), type2.ordinal());
        return ExportsGenerationType.values()[highest];
    }
}
