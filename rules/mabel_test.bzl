load("@bazel_skylib//lib:unittest.bzl", "analysistest", "asserts")
load("//rules:mabel.bzl", "DEFAULT_MAVEN_SERVERS", "TransitiveDataInfo", "artifact")

def _artifact_basic_test_impl(ctx):
    env = analysistest.begin(ctx)
    target = analysistest.target_under_test(env)

    # Check provider
    info = target[TransitiveDataInfo]
    asserts.equals(env, "inherit", info.type)

    # Check actions
    actions = analysistest.target_actions(env)
    asserts.equals(env, 1, len(actions))
    action = actions[0]
    asserts.equals(env, "MabelMavenTransitiveDependencyResolve", action.mnemonic)

    # Check arguments
    args = action.argv
    # Note: action.argv includes the executable as first argument usually?
    # Or just the arguments passed?
    # For ctx.actions.run with arguments list, argv usually contains the arguments.
    # We search for our expected flags.

    asserts.true(env, _has_arg(args, "--artifact=com.example:foo:1.0"), "Missing artifact arg")
    asserts.true(env, _has_arg(args, "--type=inherit"), "Missing type arg")
    asserts.true(env, _has_arg(args, "--debug_logs=false"), "Missing debug_logs arg")
    asserts.true(env, _has_arg(args, "--test_only=false"), "Missing test_only arg")
    asserts.true(env, _has_arg(args, "--exports_generation=inherit"), "Missing exports_generation arg")

    # Check default repository
    # It loops over repositories.
    # DEFAULT_MAVEN_SERVERS is ["https://repo1.maven.org/maven2/"]
    asserts.true(env, _has_arg(args, "--repository=https://repo1.maven.org/maven2/"), "Missing default repository")

    return analysistest.end(env)

def _artifact_custom_test_impl(ctx):
    env = analysistest.begin(ctx)
    target = analysistest.target_under_test(env)

    # Check provider
    info = target[TransitiveDataInfo]
    asserts.equals(env, "jar", info.type)

    # Check actions
    actions = analysistest.target_actions(env)
    asserts.equals(env, 1, len(actions))
    action = actions[0]

    args = action.argv

    asserts.true(env, _has_arg(args, "--artifact=com.example:bar:2.0"), "Missing artifact arg")
    asserts.true(env, _has_arg(args, "--type=jar"), "Missing type arg")
    asserts.true(env, _has_arg(args, "--debug_logs=true"), "Missing debug_logs arg")
    asserts.true(env, _has_arg(args, "--test_only=true"), "Missing test_only arg")
    asserts.true(env, _has_arg(args, "--exports_generation=none"), "Missing exports_generation arg")

    # Check excluded deps
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

    # Verify name generation logic partially
    expected_name_suffix = "com_example__foo__1_0"
    if expected_name_suffix not in basic_label:
        fail("Label name should contain mangled coordinate. Expected suffix '{}' in '{}'".format(expected_name_suffix, basic_label))

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
