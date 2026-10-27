#!/usr/bin/env bash
# Sets up extra_startup_args and extra_build_args for running Bazel in sub-workspaces (e.g. examples)

extra_startup_args=()
extra_build_args=()

if [[ -f "${PROJECT_ROOT}/tools/hermetiq-credential-helper.sh" ]]; then
  extra_startup_args+=("--bazelrc=${PROJECT_ROOT}/tools/rc/hermetiq.bazelrc")
  extra_build_args+=(
    "--credential_helper=bep.cloud-grpc.hermetiq.io=${PROJECT_ROOT}/tools/hermetiq-credential-helper.sh"
    "--credential_helper=lb.bb.cloud-grpc.hermetiq.io=${PROJECT_ROOT}/tools/hermetiq-credential-helper.sh"
    "--workspace_status_command=${PROJECT_ROOT}/tools/set-build-metadata.sh"
    "--remote_executor="
    "--remote_download_outputs=all"
    "--jobs=HOST_CPUS*2"
    "--features=-check_unknown_includes"
  )
fi

case "${CI:-}" in
  true|TRUE|1)
    extra_startup_args+=("--bazelrc=${PROJECT_ROOT}/tools/rc/ci.bazelrc")
    ;;
esac
