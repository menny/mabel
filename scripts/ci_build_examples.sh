#!/usr/bin/env bash
set -e

EXAMPLE_NAME=${1}
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# shellcheck source=scripts/setup_hermetiq_args.sh
source "${PROJECT_ROOT}/scripts/setup_hermetiq_args.sh"

cd "examples/${EXAMPLE_NAME}/"
bazel "${extra_startup_args[@]}" build "${extra_build_args[@]}" --override_module=mabel="$PROJECT_ROOT" //...
