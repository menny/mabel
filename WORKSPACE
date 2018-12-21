workspace(name="bazel_mvn_deps")

### http_archive
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file", "http_archive")

# Modified migration-toolings deps
load("//resolver:bazel_mvn_deps_dependencies.bzl", "generate_bazel_mvn_deps_workspace_rules")
generate_bazel_mvn_deps_workspace_rules()
