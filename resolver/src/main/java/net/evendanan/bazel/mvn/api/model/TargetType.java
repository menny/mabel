package net.evendanan.bazel.mvn.api.model;

public enum TargetType {
    /**
     * Plain java target.
     */
    jar,

    /**
     * Plain Android target (Java).
     */
    aar,

    /**
     * Pure Kotlin library.
     */
    kotlin,

    /**
     * Kotlin library with Android support.
     */
    kotlin_aar,

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
