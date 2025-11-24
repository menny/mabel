#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"

function verify_example() {
    local example_name="$1"
    echo "*** Verifying $example_name..."
    pushd "$example_name"
    bazel clean
    bazel build //...
    bazel run //program:main_deps
    bazel build //...
    popd
    echo "*** Success verifying $example_name"
}

verify_example plain_java
verify_example java_plugin
verify_example android
verify_example kotlin
