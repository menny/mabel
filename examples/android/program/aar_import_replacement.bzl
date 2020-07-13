"""A simple example for how to replace the aar_import rule with something that does extra stuff"""

load("@build_bazel_rules_android//android:rules.bzl", "aar_import")

#this is a macro that replaces the aar_import
# you can use this pattern to provide a different implementation of aar_import
# that does extra work (like jetifying).
# make sure you are using the exposing the same arguments as aar_import!
def macro_instead_of_aar_import(name, aar, deps, exports, tags):
    aar_import(name = name, aar = aar, deps = deps, exports = exports, tags = tags)
