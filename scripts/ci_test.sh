#!/usr/bin/env bash
set -e

bazel coverage --build_tests_only //...
curl -s https://codecov.io/bash > .codecov
chmod +x .codecov
./.codecov -t bb45a5d9-b124-469b-a8ce-695b54a0ce12 -s "bazel-out/" -G "**/coverage.dat"
