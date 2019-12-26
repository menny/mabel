workspace(name="mabel")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# rules_java defines rules for generating Java code from Protocol Buffers.
http_archive(
    name = "rules_java",
    strip_prefix = "rules_java-32ddd6c4f0ad38a54169d049ec05febc393b58fc",
    urls = [
        "https://github.com/bazelbuild/rules_java/archive/32ddd6c4f0ad38a54169d049ec05febc393b58fc.tar.gz",
    ],
)

# rules_proto defines abstract rules for building Protocol Buffers.
http_archive(
    name = "rules_proto",
    strip_prefix = "rules_proto-2c0468366367d7ed97a1f702f9cd7155ab3f73c5",
    urls = [
        "https://github.com/bazelbuild/rules_proto/archive/2c0468366367d7ed97a1f702f9cd7155ab3f73c5.tar.gz",
    ],
)

load("@rules_java//java:repositories.bzl", "rules_java_dependencies", "rules_java_toolchains")
rules_java_dependencies()
rules_java_toolchains()

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")
rules_proto_dependencies()
rules_proto_toolchains()

# Modified migration-toolings deps
load("//resolver/main_deps:dependencies.bzl", generate_bazel_mvn_deps_workspace_rules = "generate_workspace_rules")
generate_bazel_mvn_deps_workspace_rules()

