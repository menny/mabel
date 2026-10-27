#!/usr/bin/env bash
set -e

EXAMPLE_NAME=${1}
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

cd "examples/${EXAMPLE_NAME}/"

extra_startup_args=()
extra_build_args=()

if [[ -f "${PROJECT_ROOT}/tools/hermetiq-credential-helper.sh" ]]; then
  extra_startup_args+=("--bazelrc=${PROJECT_ROOT}/tools/rc/hermetiq.bazelrc")
  extra_build_args+=(
    "--credential_helper=bep.cloud-grpc.hermetiq.io=${PROJECT_ROOT}/tools/hermetiq-credential-helper.sh"
    "--credential_helper=lb.bb.cloud-grpc.hermetiq.io=${PROJECT_ROOT}/tools/hermetiq-credential-helper.sh"
    "--workspace_status_command=${PROJECT_ROOT}/tools/set-build-metadata.sh"
  )
fi

case "${CI:-}" in
  true|TRUE|1)
    extra_startup_args+=("--bazelrc=${PROJECT_ROOT}/tools/rc/ci.bazelrc")
    ;;
esac

bazel "${extra_startup_args[@]}" run "${extra_build_args[@]}" --override_module=mabel="$PROJECT_ROOT" //program:main_deps
bazel "${extra_startup_args[@]}" build "${extra_build_args[@]}" --override_module=mabel="$PROJECT_ROOT" //...
