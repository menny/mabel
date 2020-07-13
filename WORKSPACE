workspace(name = "mabel")

load("//:init_deps.bzl", "init_mabel_deps")

init_mabel_deps()

load("//:init_rules.bzl", "init_mabel_rules")

init_mabel_rules()

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

#### Below: Buildifier setup, which is only for local development
# buildifier is written in Go and hence needs rules_go to be built.
# See https://github.com/bazelbuild/rules_go for the up to date setup instructions.
http_archive(
    name = "io_bazel_rules_go",
    sha256 = "a8d6b1b354d371a646d2f7927319974e0f9e52f73a2452d2b3877118169eb6bb",
    urls = [
        "https://storage.googleapis.com/bazel-mirror/github.com/bazelbuild/rules_go/releases/download/v0.23.3/rules_go-v0.23.3.tar.gz",
        "https://github.com/bazelbuild/rules_go/releases/download/v0.23.3/rules_go-v0.23.3.tar.gz",
    ],
)

load("@io_bazel_rules_go//go:deps.bzl", "go_register_toolchains", "go_rules_dependencies")

go_rules_dependencies()

go_register_toolchains()

http_archive(
    name = "com_google_protobuf",
    sha256 = "e8c7601439dbd4489fe5069c33d374804990a56c2f710e00227ee5d8fd650e67",
    strip_prefix = "protobuf-3.11.2",
    url = "https://github.com/protocolbuffers/protobuf/archive/v3.11.2.tar.gz",
)

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")

protobuf_deps()

http_archive(
    name = "com_github_bazelbuild_buildtools",
    sha256 = "f11fc80da0681a6d64632a850346ed2d4e5cbb0908306d9a2a2915f707048a10",
    strip_prefix = "buildtools-3.3.0",
    url = "https://github.com/bazelbuild/buildtools/archive/3.3.0.tar.gz",
)
