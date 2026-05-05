#!/usr/bin/env bash
set -e

EXAMPLE_NAME=${1}
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

cd "examples/${EXAMPLE_NAME}/"
bazel run --override_module=mabel="$PROJECT_ROOT" //program:main_deps
bazel build --override_module=mabel="$PROJECT_ROOT" //...
