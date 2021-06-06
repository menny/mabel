package net.evendanan.bazel.mvn.api.model;

public enum ExportsGenerationType {
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
    none
}
