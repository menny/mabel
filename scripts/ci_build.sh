#!/usr/bin/env bash

# shellcheck disable=SC2046
bazel build $(bazel query 'attr(visibility, "//visibility:public", //...)')
