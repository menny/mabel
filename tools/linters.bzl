"Define linter aspects"

load("@aspect_rules_lint//lint:checkstyle.bzl", "lint_checkstyle_aspect")
load("@aspect_rules_lint//lint:lint_test.bzl", "lint_test")

checkstyle = lint_checkstyle_aspect(
    binary = "@@//tools:checkstyle",
    config = "@@//tools:checkstyle.xml",
    data = ["@@//tools:checkstyle-suppressions.xml"],
)

checkstyle_test = lint_test(aspect = checkstyle)
