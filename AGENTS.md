# Mabel - Agent Documentation

This document provides context and guidelines for LLM Agents working on the Mabel repository.

## Project Overview

**Mabel** (Yet another Maven dependency graph generator for Bazel) allows users to specify Maven dependencies in their Bazel project, which are then transitively resolved and locked.

**Key Mechanism:**
1.  **Resolution Phase (Java)**: A `mabel_rule` target runs a Java application (`resolver/`) that uses Maven Resolver (Aether) to resolve the dependency graph and output a JSON lockfile.
2.  **Repository Generation Phase (Starlark)**: A module extension (`rules/maven_deps/extensions.bzl`) reads the lockfile and generates an external repository containing `http_file` (or `jvm_import`/`aar_import`) rules for the artifacts.

## Repository Structure

*   **`resolver/`**: Contains the Java source code for the CLI tool that calculates the dependency graph.
    *   This is a standard Java project built with Bazel.
    *   Main classes: `Resolver.java` (resolution), `Merger.java` (graph merging).
*   **`rules/maven_deps/`**: Contains the Starlark implementation.
    *   `mabel.bzl`: Defines the `mabel_rule` and `artifact` macro.
    *   `extensions.bzl`: Defines the Bzlmod module extension.
*   **`examples/`**: Contains working examples.
    *   These act as integration tests.
    *   **CRITICAL**: `examples/local_verify_examples.sh` is the primary script to verify that changes don't break the rules.

## Build and Test

*   **Build everything**:
    ```bash
    bazel build //...
    ```
*   **Run tests**:
    ```bash
    bazel test //...
    ```
*   **Run Checkstyle**:
    ```bash
    bazel test //resolver:checkstyle
    ```
*   **Verify Examples (Integration Test)**:
    ```bash
    examples/local_verify_examples.sh
    ```
    *   *Always* run this script before submitting changes that affect the rules or the resolver logic.
    *   It cleans the examples and runs `bazel build` on them to ensure the end-to-end flow works.

## Guidelines for Agents

### 1. Bzlmod Focus
The project is fully adopting Bzlmod. When working on Bazel rules:
*   Focus on `MODULE.bazel` configuration.
*   Ensure `extensions.bzl` logic is correct for handling the lockfile and generating repositories.

### 2. Documentation Synchronization
*   The `README.md` is the source of truth for users.
*   **Rule/Macro Attributes**: If you add or modify an attribute in `mabel.bzl` (either in `mabel_rule` or `artifact`), you **MUST** update:
    1.  The docstring in `mabel.bzl`.
    2.  The "Rule Configuration" section in `README.md`.
    3.  The relevant example usage in `README.md` if applicable.

### 3. Example Maintenance
*   The `examples/` directory is not just for show; it is the test suite for the rules.
*   If you change the API (e.g., rename an attribute, change a default), you **MUST** update all relevant `BUILD.bazel` files in `examples/`.
*   Do not remove examples unless explicitly instructed.

### 4. Code Style
*   **Java**: Follow standard Java conventions. Checkstyle is enforced (`bazel test //resolver:checkstyle`).
*   **Starlark**: Follow Bazel best practices. Use `load()` statements correctly.
*   **Markdown**: partial to GitHub Flavored Markdown. Keep it clean and readable.

### 5. Implementation Details
*   **Android Support**: `mabel_rule` detects `aar` files and generates `aar_import`.
*   **Kotlin Support**: Works seamlessly via standard JVM rules.
*   **Lockfile**: The JSON lockfile is the interface between the resolution phase and the repository generation phase. Its structure is critical.

