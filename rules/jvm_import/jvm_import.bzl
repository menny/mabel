# Stripped down version of a java_import Starlark rule, without invoking ijar
# to create interface jars.

# Inspired by Square's implementation of `raw_jvm_import` [0] and discussions
# on the GitHub thread [1] about ijar's interaction with Kotlin JARs.
#
# This file was taken from [2] jvm_external repo (under Apache2 license [3]).
#
# [0]: https://github.com/square/bazel_maven_repository/pull/48
# [1]: https://github.com/bazelbuild/bazel/issues/4549
# [2]: https://github.com/bazelbuild/rules_jvm_external/blob/aa530f0c37d86d18040df82ca7e5de7a4732f861/private/rules/jvm_import.bzl
# [3]: https://github.com/bazelbuild/rules_jvm_external/blob/aa530f0c37d86d18040df82ca7e5de7a4732f861/LICENSE

load(":stamp_manifest.bzl", "StampManifestProvider")

def _jvm_import_impl(ctx):
    if len(ctx.files.jars) != 1:
        fail("Please only specify one jar to import in the jars attribute.")

    injar = ctx.files.jars[0]
    if ctx.attr._stamp_manifest[StampManifestProvider].stamp_enabled:
        outjar = ctx.actions.declare_file("processed_" + injar.basename, sibling = injar)
        args = ctx.actions.args()
        args.add_all(["--source", injar, "--output", outjar])
        args.add_all(["--manifest-entry", "Target-Label:{target_label}".format(target_label = ctx.label)])
        ctx.actions.run(
            executable = ctx.executable._add_jar_manifest_entry,
            arguments = [args],
            inputs = [injar],
            outputs = [outjar],
            mnemonic = "StampJarManifest",
            progress_message = "Stamping the manifest of %s" % ctx.label,
        )
    else:
        outjar = injar

    compilejar = ctx.actions.declare_file("header_" + injar.basename)
    args = ctx.actions.args()
    args.add_all(["--source", outjar, "--output", compilejar])

    # We need to remove the `Class-Path` entry since bazel 4.0.0 forces `javac`
    # to run `-Xlint:path` no matter what other flags are passed. Bazel
    # manages the classpath for us, so the `Class-Path` manifest entry isn't
    # needed. Worse, if it's there and the jars listed in it aren't found,
    # the lint check will emit a `bad path element` warning. We get quiet and
    # correct builds if we remove the `Class-Path` manifest entry entirely.
    args.add_all(["--remove-entry", "Class-Path"])
    ctx.actions.run(
        executable = ctx.executable._add_jar_manifest_entry,
        arguments = [args],
        inputs = [outjar],
        outputs = [compilejar],
        mnemonic = "CreateCompileJar",
        progress_message = "Creating compile jar for %s" % ctx.label,
    )

    return [
        DefaultInfo(
            files = depset([outjar]),
        ),
        JavaInfo(
            compile_jar = compilejar,
            output_jar = outjar,
            source_jar = ctx.file.srcjar,
            deps = [
                dep[JavaInfo]
                for dep in ctx.attr.deps
                if JavaInfo in dep
            ],
            runtime_deps = [
                dep[JavaInfo]
                for dep in ctx.attr.runtime_deps
                if JavaInfo in dep
            ],
            neverlink = ctx.attr.neverlink,
        ),
    ]

jvm_import = rule(
    attrs = {
        "jars": attr.label_list(
            allow_files = True,
            allow_empty = False,
            mandatory = True,
            cfg = "target",
        ),
        "srcjar": attr.label(
            allow_single_file = True,
            mandatory = False,
            cfg = "target",
        ),
        "licenses": attr.string_list(mandatory = False, allow_empty = True, default = [], doc = ""),
        "deps": attr.label_list(
            default = [],
            providers = [JavaInfo],
        ),
        "exports": attr.label_list(
            default = [],
            providers = [JavaInfo],
        ),
        "runtime_deps": attr.label_list(
            default = [],
            providers = [JavaInfo],
        ),
        "neverlink": attr.bool(
            default = False,
        ),
        "_add_jar_manifest_entry": attr.label(
            executable = True,
            cfg = "exec",
            default = "//rules/jvm_import:AddJarManifestEntry",
        ),
        "_stamp_manifest": attr.label(
            default = "//rules/jvm_import:stamp_manifest",
        ),
    },
    implementation = _jvm_import_impl,
    provides = [JavaInfo],
)
