#!/usr/bin/env bash
set -e

bazel run //resolver:main_deps
bazel run //resolver:main_deps
bazel build //resolver/src/...
