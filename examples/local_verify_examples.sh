#!/bin/sh
(cd plain_java ; bazel clean ; bazel build //... ; bazel run //program:main_deps ; bazel build)
(cd java_plugin ; bazel clean ; bazel build //... ; bazel run //program:main_deps ; bazel build)
(cd android ; bazel clean ; bazel build //... ; bazel run //program:main_deps ; bazel build)
(cd kotlin ; bazel clean ; bazel build //... ; bazel run //program:main_deps ; bazel build)
#on purpose, not building this
(cd kotlin_with_kt_rules ; bazel clean ; bazel run //program:main_deps)
