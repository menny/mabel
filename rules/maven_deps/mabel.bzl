"""Defining mabel bazel rules."""
TransitiveDataInfo = provider(doc = "Internal provider for connectin resolving and merging.", fields = ["graph_file", "type"])

def _impl_resolver(ctx):
    output_file = ctx.outputs.out
    java_runtime = ctx.attr._jdk[java_common.JavaRuntimeInfo]
    java_home = java_runtime.java_home_runfiles_path

    arguments = ["--repository={}".format(repository) for repository in ctx.attr.repositories] + \
                ["--blacklist={}".format(exclude_artifact_list) for exclude_artifact_list in ctx.attr.maven_exclude_deps] + \
                [
                    "--artifact={}".format(ctx.attr.coordinate),
                    "--type={}".format(ctx.attr.type),
                    "--output_file={}".format(output_file.path),
                    "--debug_logs={}".format(ctx.attr.debug_logs).lower(),
                    "--test_only={}".format(ctx.attr.test_only).lower(),
                    "--jdk_home={}".format(java_home),
                ]

    ctx.actions.run(
        outputs = [output_file],
        executable = ctx.executable._resolver,
        arguments = arguments,
        mnemonic = "MabelMavenTransitiveDependencyResolve",
    )

    return [TransitiveDataInfo(graph_file = output_file, type = ctx.attr.type)]

DEFAULT_MAVEN_SERVERS = ["https://repo1.maven.org/maven2/"]

_mabel_maven_dependency_graph_resolving_rule = rule(
    implementation = _impl_resolver,
    doc = "Generates a file that represents this Maven dependency and its transitive dependencies.",
    attrs = {
        "coordinate": attr.string(mandatory = True, doc = "Maven coordinate in the form of `group-id:artifact-id:version`."),
        "debug_logs": attr.bool(default = False, doc = "If set to True, will print out debug logs while resolving dependencies. Default is False.", mandatory = False),
        "maven_exclude_deps": attr.string_list(allow_empty = True, default = [], doc = "List of Maven dependencies which should not be resolved. You can omit the `version` or both `artifact-id:version`."),
        "repositories": attr.string_list(allow_empty = False, default = DEFAULT_MAVEN_SERVERS, doc = "List of URLs that point to Maven servers. Defaut is Maven-Central."),
        "test_only": attr.bool(default = False, doc = "Should this artifact be marked as test_only. Default is False.", mandatory = False),
        "type": attr.string(mandatory = True, default = "inherit", values = ["inherit", "jar", "aar", "kotlin", "kotlin_aar", "naive", "processor", "auto"], doc = "The type of artifact targets to generate."),
        "_jdk": attr.label(default = Label("@bazel_tools//tools/jdk:remote_jdk11"), providers = [java_common.JavaRuntimeInfo]),
        "_resolver": attr.label(executable = True, allow_files = True, cfg = "host", default = Label("//resolver:resolver_bin")),
    },
    outputs = {"out": "%{name}-transitive-graph.data"},
)

# buildifier: disable=unnamed-macro
def artifact(coordinate, maven_exclude_deps = [], repositories = DEFAULT_MAVEN_SERVERS, debug_logs = False, type = "inherit", exports_generation = "inherit", test_only = False):
    rule_name = "_mabel_maven_dependency_graph_resolving_{}".format(coordinate.replace(":", "__").replace("-", "_").replace(".", "_"))

    # different targets may use the same artifact
    if native.existing_rule(rule_name) == None:
        _mabel_maven_dependency_graph_resolving_rule(
            name = rule_name,
            coordinate = coordinate,
            type = type,
            maven_exclude_deps = maven_exclude_deps,
            repositories = repositories,
            test_only = test_only,
            debug_logs = debug_logs,
            visibility = ["//visibility:private"],
        )
    return ":%s" % rule_name

# buildifier: enabled=unnamed-macro

script_merger_template = """
{java} -jar {merger} {graph_files_list} \
    --output_macro_file_path={output_filename} \
    --output_target_build_files_base_path=${{BUILD_WORKING_DIRECTORY}}/{output_target_build_files_base_path} \
    --calculate_sha={calculate_sha} \
    --fetch_srcjar={fetch_srcjar} \
    --package_path={package_path} \
    --rule_prefix={rule_prefix} \
    --debug_logs={debug_logs} \
    --create_deps_sub_folders={create_deps_sub_folders} \
    --output_pretty_dep_graph_filename={output_pretty_dep_graph_filename} \
    --artifacts_path={artifacts_path} \
    --public_targets_category={public_targets_category} \
    --version_conflict_resolver={version_conflict_resolver} \
    --type={default_target_type} \
    --exports_generation={default_exports_generation} \
    --keep_output_folder={keep_output_folder}

echo "Stored resolved dependencies graph (rules) at ${{BUILD_WORKING_DIRECTORY}}/{output_target_build_files_base_path}{output_filename}"
"""

def _impl_merger(ctx):
    output_filename = "dependencies.bzl"
    output_target_build_files_base_path = "{}/{}/".format(ctx.label.package, ctx.label.name)
    package_path = ctx.label.package
    source_files = [dep[TransitiveDataInfo].graph_file for dep in ctx.attr.maven_deps]
    script = ctx.outputs.out
    java_runtime = ctx.attr._jdk[java_common.JavaRuntimeInfo]
    java_path = str(java_runtime.java_executable_exec_path)

    script_content = script_merger_template.format(
        java = java_path,
        merger = ctx.executable._merger.short_path,
        graph_files_list = " ".join(["--graph_file={}".format(file.short_path) for file in source_files]),
        output_filename = output_filename,
        output_target_build_files_base_path = output_target_build_files_base_path,
        package_path = package_path,
        fetch_srcjar = "{}".format(ctx.attr.fetch_srcjar).lower(),
        calculate_sha = "{}".format(ctx.attr.calculate_sha).lower(),
        debug_logs = "{}".format(ctx.attr.debug_logs).lower(),
        rule_prefix = ctx.attr.generated_targets_prefix,
        artifacts_path = "~/.mabel/artifacts/".format("~") if ctx.attr.artifacts_path == "" else ctx.attr.artifacts_path,
        output_pretty_dep_graph_filename = "dependencies.txt" if ctx.attr.output_graph_to_file else "",
        create_deps_sub_folders = "{}".format(ctx.attr.generate_deps_sub_folder).lower(),
        public_targets_category = ctx.attr.public_targets_category,
        version_conflict_resolver = ctx.attr.version_conflict_resolver,
        keep_output_folder = "{}".format(ctx.attr.keep_output_folder).lower(),
        default_exports_generation = ctx.attr.default_exports_generation,
        default_target_type = ctx.attr.default_target_type,
    )

    ctx.actions.write(script, script_content, is_executable = True)

    return [DefaultInfo(executable = script, runfiles = ctx.runfiles(files = [ctx.executable._merger] + source_files + ctx.files._jdk))]

mabel_rule = rule(
    implementation = _impl_merger,
    doc = """Generates a bzl file with repository-rules and targets which describes a Maven dependecy graph based on
     the provided `maven_deps` values. The result will be stored in a `bzl` file in a sub-folder named the same as this rule target's name.
     The generated file will contain two macros:

     * `generate_XXX_workspace_rules` - should be load and run in the `WORKSPACE` file. This will create repository-rules for all the remote Maven artifacts (jars).
     * `generate_XXX_transitive_dependency_rules` - should be loaded in the relavent `BUILD.bazel` file. This will create targets for each of the requested `maven_deps`.

     Additionally, a sub-folder structure will also be generated
     """,
    executable = True,
    attrs = {
        "artifacts_path": attr.string(default = "", doc = "Cache location to download artifacts into. Empty means `[user-home-folder]/.mabel/artifacts/`", mandatory = False),
        "calculate_sha": attr.bool(default = True, doc = "Will also calculate SHA256 of the artifact. Default True", mandatory = False),
        "debug_logs": attr.bool(default = False, doc = "If set to True, will print out debug logs while resolving dependencies. Default is False.", mandatory = False),
        "default_exports_generation": attr.string(default = "requested_deps", values = ["all", "requested_deps", "none"], doc = "For which targets should we generate exports attribute."),
        "default_target_type": attr.string(default = "auto", values = ["jar", "aar", "kotlin", "kotlin_aar", "naive", "processor", "auto"], doc = "The type of artifact targets to generate."),
        "fetch_srcjar": attr.bool(default = False, doc = "Will also try to locate srcjar for the dependency. Default False", mandatory = False),
        "generate_deps_sub_folder": attr.bool(default = True, doc = "If set to True (the default), will create sub-folders with BUILD.bazel file for each dependency.", mandatory = False),
        "generated_targets_prefix": attr.string(default = "", doc = "A prefix to add to all generated targets. Default is an empty string, meaning no prefix.", mandatory = False),
        "keep_output_folder": attr.bool(default = False, doc = "If set to False (the default), will first remove the output folder.", mandatory = False),
        "maven_deps": attr.label_list(
            mandatory = True,
            allow_empty = False,
            providers = [TransitiveDataInfo],
            doc = "List of `maven_dependency_graph_rule` targets.",
        ),
        "output_graph_to_file": attr.bool(default = False, doc = "If set to True, will output the graph to dependencies.txt. Default is False.", mandatory = False),
        "public_targets_category": attr.string(mandatory = False, default = "all", values = ["requested_deps", "recursive_exports", "all"], doc = "Set public visibility of resolved targets. Default is 'all'. Can be: 'requested_deps', 'recursive_exports', 'all'."),
        "version_conflict_resolver": attr.string(mandatory = False, default = "latest_version", values = ["latest_version", "breadth_first"], doc = "Defines the strategy used to resolve version-conflicts. Default is 'latest_version'. Can be: 'latest_version', 'breadth_first'."),
        "_jdk": attr.label(default = Label("@bazel_tools//tools/jdk:remote_jdk11"), providers = [java_common.JavaRuntimeInfo]),
        "_merger": attr.label(executable = True, allow_single_file = True, cfg = "host", default = Label("//resolver:merger_bin_deploy.jar")),
    },
    outputs = {"out": "%{name}-generate-deps.sh"},
)
