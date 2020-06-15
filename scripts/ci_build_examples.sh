#!/usr/bin/env bash
set -e

EXAMPLE_NAME=${1}

cd "examples/${EXAMPLE_NAME}/"
bazel build //...
