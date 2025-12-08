"""Tests for mabel.bzl artifact macro."""

load("@bazel_skylib//lib:unittest.bzl", "analysistest", "asserts")
load("//rules:mabel.bzl", "DEFAULT_MAVEN_SERVERS", "TransitiveDataInfo", "artifact")

def _artifact_basic_test_impl(ctx):
    env = analysistest.begin(ctx)
    target = analysistest.target_under_test(env)

    # Check provider
    info = target[TransitiveDataInfo]
    asserts.equals(env, "inherit", info.type)

    # Check actions - high level check
    actions = analysistest.target_actions(env)

    # Filter out BaselineCoverage action which is present when running with coverage
    actions = [a for a in actions if a.mnemonic != "BaselineCoverage"]
    asserts.equals(env, 1, len(actions))
    action = actions[0]
    asserts.equals(env, "MabelMavenTransitiveDependencyResolve", action.mnemonic)

    # Check critical arguments
    args = action.argv
    asserts.true(env, _has_arg(args, "--artifact=com.example:foo:1.0"), "Missing artifact arg")
    asserts.true(env, _has_arg(args, "--type=inherit"), "Missing type arg")

    return analysistest.end(env)

def _artifact_custom_test_impl(ctx):
    env = analysistest.begin(ctx)
    target = analysistest.target_under_test(env)

    # Check provider
    info = target[TransitiveDataInfo]
    asserts.equals(env, "jar", info.type)

    # Check actions
    actions = analysistest.target_actions(env)

    # Filter out BaselineCoverage action which is present when running with coverage
    actions = [a for a in actions if a.mnemonic != "BaselineCoverage"]
    asserts.equals(env, 1, len(actions))
    action = actions[0]

    args = action.argv

    # Check critical arguments
    asserts.true(env, _has_arg(args, "--artifact=com.example:bar:2.0"), "Missing artifact arg")
    asserts.true(env, _has_arg(args, "--type=jar"), "Missing type arg")

    # Check custom values that change behavior
    asserts.true(env, _has_arg(args, "--test_only=true"), "Missing test_only arg")
    asserts.true(env, _has_arg(args, "--blacklist=group:exclude"), "Missing blacklist arg")

    return analysistest.end(env)

def _has_arg(args, expected):
    for arg in args:
        if arg == expected:
            return True
    return False

artifact_basic_test = analysistest.make(_artifact_basic_test_impl)
artifact_custom_test = analysistest.make(_artifact_custom_test_impl)

def mabel_test_suite():
    # Test 1: Basic usage
    basic_label = artifact("com.example:foo:1.0")

    artifact_basic_test(
        name = "artifact_basic_test",
        target_under_test = basic_label,
    )

    # Test 2: Custom attributes
    custom_label = artifact(
        coordinate = "com.example:bar:2.0",
        type = "jar",
        debug_logs = True,
        test_only = True,
        exports_generation_type = "none",
        maven_exclude_deps = ["group:exclude"],
    )

    artifact_custom_test(
        name = "artifact_custom_test",
        target_under_test = custom_label,
    )

    # Test 3: Deduplication
    dup_label = artifact("com.example:foo:1.0")

    if basic_label != dup_label:
        fail("artifact() macro should be idempotent and return the same label for the same coordinate. Expected {}, got {}".format(basic_label, dup_label))
