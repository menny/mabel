"""
buildifier support
"""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file")

_BUILDIFIER_VERSION = "6.1.0"
_BUILDIFIER_URL = "https://github.com/bazelbuild/buildtools/releases/download"

def init_buildifier():
    """
    Initializing buildifier
    """

    # buildifier is written in Go and hence needs rules_go to be built.
    # See https://github.com/bazelbuild/rules_go for the up to date setup instructions.
    http_archive(
        name = "io_bazel_rules_go",
        sha256 = "d6b2513456fe2229811da7eb67a444be7785f5323c6708b38d851d2b51e54d83",
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/rules_go/releases/download/v0.30.0/rules_go-v0.30.0.zip",
            "https://github.com/bazelbuild/rules_go/releases/download/v0.30.0/rules_go-v0.30.0.zip",
        ],
    )

    http_archive(
        name = "com_github_bazelbuild_buildtools",
        sha256 = "a75c337f4d046e560298f52ae95add73b9b933e4d6fb01ed86d57313e53b68e6",
        strip_prefix = "buildtools-{}".format(_BUILDIFIER_VERSION),
        urls = [
            "https://github.com/bazelbuild/buildtools/archive/refs/tags/{}.tar.gz".format(_BUILDIFIER_VERSION),
        ],
    )

    http_file(
        name = "buildifier_darwin_amd64",
        url = "{}/{}/buildifier-darwin-amd64".format(_BUILDIFIER_URL, _BUILDIFIER_VERSION),
        sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b812",
        downloaded_file_path = "buildifier",
        executable = True,
    )
    http_file(
        name = "buildifier_darwin_arm64",
        url = "{}/{}/buildifier-darwin-amd64".format(_BUILDIFIER_URL, _BUILDIFIER_VERSION),
        sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852a812",
        downloaded_file_path = "buildifier",
        executable = True,
    )
    http_file(
        name = "buildifier_linux_amd64",
        url = "{}/{}/buildifier-linux-amd64".format(_BUILDIFIER_URL, _BUILDIFIER_VERSION),
        sha256 = "0b51a6cb81bc3b51466ea2210053992654987a907063d0c2b9c03be29de52eff",
        downloaded_file_path = "buildifier",
        executable = True,
    )
    http_file(
        name = "buildifier_linux_arm64",
        url = "{}/{}/buildifier-linux-arm64".format(_BUILDIFIER_URL, _BUILDIFIER_VERSION),
        sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495911b7852b812",
        downloaded_file_path = "buildifier",
        executable = True,
    )
    http_file(
        name = "buildifier_windows_amd64",
        url = "{}/{}/buildifier-windows-amd64.exe".format(_BUILDIFIER_URL, _BUILDIFIER_VERSION),
        sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495911b1852b112",
        downloaded_file_path = "buildifier.exe",
        executable = True,
    )
