"""mabel dependencies macro. Should be called before init_mabel_deps was called"""

load("//resolver/main_deps:dependencies.bzl", generate_bazel_mvn_deps_workspace_rules = "generate_workspace_rules")

def init_mabel_rules():
    generate_bazel_mvn_deps_workspace_rules()
