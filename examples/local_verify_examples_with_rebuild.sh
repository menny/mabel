#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"
PROJECT_ROOT="$(cd .. && pwd)"

function verify_example() {
    local example_name="$1"
    echo "*** Verifying $example_name..."
    pushd "$example_name"
    bazel clean
    bazel build --override_module=mabel="$PROJECT_ROOT" //...
    bazel run --override_module=mabel="$PROJECT_ROOT" //program:main_deps
    bazel build --override_module=mabel="$PROJECT_ROOT" //...
    bazel clean --expunge
    popd
    echo "*** Success verifying $example_name"
}

verify_example plain_java
verify_example java_plugin
verify_example kotlin
verify_example android
verify_example android-kotlin
