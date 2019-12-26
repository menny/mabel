load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def init_mabel_deps():
    # rules_java defines rules for generating Java code from Protocol Buffers.
    http_archive(
        name = "rules_java",
        strip_prefix = "rules_java-32ddd6c4f0ad38a54169d049ec05febc393b58fc",
        sha256 = "1969a89e8da396eb7754fd0247b7df39b6df433c3dcca0095b4ba30a5409cc9d",
        urls = [
            "https://github.com/bazelbuild/rules_java/archive/32ddd6c4f0ad38a54169d049ec05febc393b58fc.tar.gz",
        ],
    )

    # rules_proto defines abstract rules for building Protocol Buffers.
    http_archive(
        name = "rules_proto",
        strip_prefix = "rules_proto-2c0468366367d7ed97a1f702f9cd7155ab3f73c5",
        sha256 = "73ebe9d15ba42401c785f9d0aeebccd73bd80bf6b8ac78f74996d31f2c0ad7a6",
        urls = [
            "https://github.com/bazelbuild/rules_proto/archive/2c0468366367d7ed97a1f702f9cd7155ab3f73c5.tar.gz",
        ],
    )
