load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//rules:extensions.bzl", "get_file_path_from_maven_name", "parse_maven_coordinate")

def _get_file_path_from_maven_name_test(ctx):
    env = unittest.begin(ctx)

    asserts.equals(
        env,
        "com/google/guava/guava",
        get_file_path_from_maven_name("com.google.guava", "guava"),
    )
    asserts.equals(
        env,
        "simple/artifact",
        get_file_path_from_maven_name("simple", "artifact"),
    )

    return unittest.end(env)

def _parse_maven_coordinate_test(ctx):
    env = unittest.begin(ctx)

    # Test valid inputs
    group, artifact, version = parse_maven_coordinate("com.google.guava:guava:20.0")
    asserts.equals(env, "com.google.guava", group)
    asserts.equals(env, "guava", artifact)
    asserts.equals(env, "20.0", version)

    group, artifact, version = parse_maven_coordinate("group:artifact:1.0:type")
    asserts.equals(env, "group", group)
    asserts.equals(env, "artifact", artifact)
    asserts.equals(env, "1.0", version)

    return unittest.end(env)

get_file_path_test = unittest.make(_get_file_path_from_maven_name_test)
parse_coordinate_test = unittest.make(_parse_maven_coordinate_test)

def extensions_test_suite():
    unittest.suite(
        "extensions_tests",
        get_file_path_test,
        parse_coordinate_test,
    )
