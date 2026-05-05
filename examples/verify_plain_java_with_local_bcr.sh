#!/usr/bin/env bash
set -e

# This script verifies the plain_java example using a local Bazel Central Registry (BCR).
# Usage: ./verify_plain_java_with_local_bcr.sh /path/to/local/bazel-central-registry

if [ -z "$1" ]; then
    echo "Usage: $0 /path/to/local/bazel-central-registry"
    exit 1
fi

BCR_PATH=$(realpath "$1")

cd "$(dirname "$0")/plain_java"

echo "*** Verifying plain_java with local BCR at $BCR_PATH..."

# We point to the local BCR.
BAZEL_FLAGS=(
    "--registry=file://$BCR_PATH"
    "--registry=https://bcr.bazel.build"
)

echo "*** Initial build..."
bazel build "${BAZEL_FLAGS[@]}" //...

echo "*** Regenerating dependencies..."
bazel run "${BAZEL_FLAGS[@]}" //program:main_deps

echo "*** Rebuilding after regeneration..."
bazel build "${BAZEL_FLAGS[@]}" //...

echo "*** Success verifying plain_java with local BCR"
