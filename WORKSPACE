workspace(name="bazel_mvn_deps")

### http_archive
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file", "http_archive")

# Modified migration-toolings deps
load("//others/migration-tooling:dependencies.bzl", "generate_migration_tools_workspace_rules")
generate_migration_tools_workspace_rules()
