load("@rules_java//java:repositories.bzl", "rules_java_dependencies", "rules_java_toolchains")
load("//resolver/main_deps:dependencies.bzl", generate_bazel_mvn_deps_workspace_rules = "generate_workspace_rules")

def init_mabel_rules():
    rules_java_dependencies()
    rules_java_toolchains()

    generate_bazel_mvn_deps_workspace_rules()
