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

*   **BAZEL build system**: Ensure you keep the `BUILD.bazel` files in sync with the the code changes, new targets, removed targets or files, etc.

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

## Lint and Format

- don't try to fix linting or formatting issues, we have auto-fixers for that. This is applicable for _all_ code in the codebase.
- You can run the auto-fixers with `bazel run //tools:format`. This is applicable for _all_ code in the codebase.

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

### Commit Message

Before creating a commit always run `bazel run //tools:format`.

When creating a commit message, follow these guidelines:

- **Title:** Use a concise title. Prefix the title with "[LLM]"
- **Description:** The description should include a short description of the issue (bug, feature-request, crash, chore, etc) and a short description of the solution. Add your name at the end of the description to signify the commit was made by an AI Agent.

### Tests

- when ask to suggest tests for a function or file:
  - Do not implement anything or suggest how to implement.
  - You should only look at the code and suggest tests based on functionality and error cases.
  - Identify the "happy path" - core functionality - cases and mark them as such in your suggestions
  - Identify the error cases and mark them as such in your suggestions. Estimate importance based on likelyhood.
  - Identify the edge cases and mark them as such in your suggestions. Estimate importance based on likelyhood.
- when implementing tests:
  - For Java, the test file name follows the pattern `[original_file_name]Test.java`
  - The files are located under the `src/test` root directory of the same package as the original file.
    - for example, if the original file is `resolver/src/main/java/net/evendanan/bazel/mvn/impl/JsonLockfileWriter.java`, the test file should be `resolver/src/test/java/net/evendanan/bazel/mvn/impl/JsonLockfileWriterTest.java`
  - prefer creating fakes over mocks or patches. But, if it is simpler to patch or mock, do that.
  - For Starlark (bz, bzl), use `bazel_skylib`'s `unittest` framework:
    - The test file name follows the pattern `[original_file_name]_test.bzl` and must be located in the same directory as the original file.
    - You may export private functions (functions starting with `_`) to be public (remove the `_` prefix) to be able to test them, but only do so if strictly necessary.
    - Create a test suite function (e.g., `extensions_test_suite()`) in the test file that aggregates all test cases.
    - In the `BUILD.bazel` file, load the test suite and execute it.
    - Ensure `bazel_skylib` is available in `MODULE.bazel`.

### Naming

- use inclusive language when creating variables, functions, class names, stubs, etc:
  - Do not use "dummy", instead use "fake", "mock", "noop" etc.
  - Do not use "blacklist", instead use "disallow-list"
  - Do not use "whilelist", instead use "allow-list"
  - Stay away from: "master", "slave", "insane", "dumb", etc.
  - Use gender neutral pronouns
