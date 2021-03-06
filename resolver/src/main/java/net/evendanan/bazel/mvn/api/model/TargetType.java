package net.evendanan.bazel.mvn.api.model;

public enum TargetType {
    /**
     * Use default value.
     */
    inherit,

    /**
     * POM file import (when there is no jar, just dependencies definitions).
     */
    pom,

    /**
     * Plain java target.
     */
    jar,

    /**
     * Plain Android target (Java).
     */
    aar,

    /**
     * Very fast detector for pom, jar, aar, kotlin and kotlin-aar. Does not detect annotation-processors.
     */
    naive,

    /**
     * Java annotation-processor target (java-plugin).
     */
    processor,

    /**
     * Slow detector. Will inspect the artifact and determine what it is.
     */
    auto
}
