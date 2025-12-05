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
                    "--exports_generation={}".format(ctx.attr.exports_generation_type),
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
        "exports_generation_type": attr.string(mandatory = True, default = "inherit", values = ["inherit", "all", "requested_deps", "none"], doc = "For which targets should we generate exports attribute."),
        "maven_exclude_deps": attr.string_list(allow_empty = True, default = [], doc = "List of Maven dependencies which should not be resolved. You can omit the `version` or both `artifact-id:version`."),
        "repositories": attr.string_list(allow_empty = False, default = DEFAULT_MAVEN_SERVERS, doc = "List of URLs that point to Maven servers. Defaut is Maven-Central."),
        "test_only": attr.bool(default = False, doc = "Should this artifact be marked as test_only. Default is False.", mandatory = False),
        "type": attr.string(mandatory = True, default = "inherit", values = ["inherit", "jar", "aar", "naive", "processor", "auto"], doc = "The type of artifact targets to generate."),
        "_jdk": attr.label(default = Label("@bazel_tools//tools/jdk:current_java_runtime"), providers = [java_common.JavaRuntimeInfo]),
        "_resolver": attr.label(executable = True, allow_files = True, cfg = "exec", default = Label("//resolver:resolver_bin")),
    },
    outputs = {"out": "%{name}-transitive-graph.data"},
)

# buildifier: disable=unnamed-macro
def artifact(coordinate, maven_exclude_deps = [], repositories = DEFAULT_MAVEN_SERVERS, debug_logs = False, type = "inherit", exports_generation_type = "inherit", test_only = False):
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
            exports_generation_type = exports_generation_type,
            debug_logs = debug_logs,
            visibility = ["//visibility:private"],
        )
    return ":%s" % rule_name

# buildifier: enabled=unnamed-macro

script_merger_template = """
{java} -jar {merger} {graph_files_list} \
    --repository_rule_name={repository_rule_name} \
    --calculate_sha={calculate_sha} \
    --fetch_srcjar={fetch_srcjar} \
    --rule_prefix={rule_prefix} \
    --debug_logs={debug_logs} \
    --output_pretty_dep_graph_filename={output_pretty_dep_graph_filename} \
    --artifacts_path={artifacts_path} \
    --public_targets_category={public_targets_category} \
    --version_conflict_resolver={version_conflict_resolver} \
    --type={default_target_type} \
    --exports_generation={default_exports_generation} \
    {lockfile_param}

echo "Stored resolved dependencies graph (rules) at {output_pretty_dep_graph_filename}"{lockfile_echo}
"""

def _impl_merger(ctx):
    source_files = [dep[TransitiveDataInfo].graph_file for dep in ctx.attr.maven_deps]
    script = ctx.outputs.out
    java_runtime = ctx.attr._jdk[java_common.JavaRuntimeInfo]

    # Use runfiles path for bazel run - this will be available in the runfiles tree
    java_path = "{}/bin/java".format(java_runtime.java_home_runfiles_path)

    # Prepare lockfile parameters
    lockfile_param = ""
    lockfile_echo = ""
    if ctx.attr.lockfile_path:
        lockfile_param = " \\\n    --lockfile_path=${{BUILD_WORKING_DIRECTORY}}/{}".format(ctx.attr.lockfile_path)
        lockfile_echo = "\necho \"Stored lockfile at ${{BUILD_WORKING_DIRECTORY}}/{}\"".format(ctx.attr.lockfile_path)

    output_pretty_dep_graph_filename = ""
    if ctx.attr.output_graph_to_file:
        output_pretty_dep_graph_filename = "${{BUILD_WORKING_DIRECTORY}}/{}/dependencies.txt".format(ctx.label.package)

    script_content = script_merger_template.format(
        java = java_path,
        merger = ctx.executable._merger.short_path,
        graph_files_list = " ".join(["--graph_file={}".format(file.short_path) for file in source_files]),
        repository_rule_name = ctx.attr.mabel_repository_rule_name,
        fetch_srcjar = "{}".format(ctx.attr.fetch_srcjar).lower(),
        calculate_sha = "{}".format(ctx.attr.calculate_sha).lower(),
        debug_logs = "{}".format(ctx.attr.debug_logs).lower(),
        rule_prefix = ctx.attr.generated_targets_prefix,
        artifacts_path = "~/.mabel/artifacts/".format("~") if ctx.attr.artifacts_path == "" else ctx.attr.artifacts_path,
        output_pretty_dep_graph_filename = output_pretty_dep_graph_filename,
        public_targets_category = ctx.attr.public_targets_category,
        version_conflict_resolver = ctx.attr.version_conflict_resolver,
        default_exports_generation = ctx.attr.default_exports_generation,
        default_target_type = ctx.attr.default_target_type,
        lockfile_param = lockfile_param,
        lockfile_echo = lockfile_echo,
    )

    ctx.actions.write(script, script_content, is_executable = True)

    return [DefaultInfo(executable = script, runfiles = ctx.runfiles(files = [ctx.executable._merger] + source_files + ctx.files._jdk))]

mabel_rule = rule(
    implementation = _impl_merger,
    doc = """Generates a JSON lockfile which describes a Maven dependecy graph based on the provided `maven_deps` values.""",
    executable = True,
    attrs = {
        "artifacts_path": attr.string(default = "", doc = "Cache location to download artifacts into. Empty means `[user-home-folder]/.mabel/artifacts/`", mandatory = False),
        "calculate_sha": attr.bool(default = True, doc = "Will also calculate SHA256 of the artifact. Default True", mandatory = False),
        "debug_logs": attr.bool(default = False, doc = "If set to True, will print out debug logs while resolving dependencies. Default is False.", mandatory = False),
        "default_exports_generation": attr.string(default = "requested_deps", values = ["all", "requested_deps", "none"], doc = "For which targets should we generate exports attribute."),
        "default_target_type": attr.string(default = "auto", values = ["jar", "aar", "naive", "processor", "auto"], doc = "The type of artifact targets to generate."),
        "fetch_srcjar": attr.bool(default = False, doc = "Will also try to locate srcjar for the dependency. Default False", mandatory = False),
        "generated_targets_prefix": attr.string(default = "", doc = "A prefix to add to all generated targets. Default is an empty string, meaning no prefix.", mandatory = False),
        "lockfile_path": attr.string(default = "", doc = "Path to output JSON lockfile for bzlmod. Path is relative to workspace root.", mandatory = False),
        "mabel_repository_rule_name": attr.string(mandatory = False, default = "mabel", doc = "The name of the mabel remote-repository name (the name of the `http_archive` used to import _mabel_). Default is `mabel`."),
        "maven_deps": attr.label_list(
            mandatory = True,
            allow_empty = False,
            providers = [TransitiveDataInfo],
            doc = "List of `maven_dependency_graph_rule` targets.",
        ),
        "output_graph_to_file": attr.bool(default = False, doc = "If set to True, will output the graph to dependencies.txt. Default is False.", mandatory = False),
        "public_targets_category": attr.string(mandatory = False, default = "all", values = ["requested_deps", "recursive_exports", "all"], doc = "Set public visibility of resolved targets. Default is 'all'. Can be: 'requested_deps', 'recursive_exports', 'all'."),
        "version_conflict_resolver": attr.string(mandatory = False, default = "latest_version", values = ["latest_version", "breadth_first"], doc = "Defines the strategy used to resolve version-conflicts. Default is 'latest_version'. Can be: 'latest_version', 'breadth_first'."),
        "_jdk": attr.label(default = Label("@bazel_tools//tools/jdk:current_java_runtime"), providers = [java_common.JavaRuntimeInfo]),
        "_merger": attr.label(executable = True, allow_single_file = True, cfg = "exec", default = Label("//resolver:merger_bin_deploy.jar")),
    },
    outputs = {"out": "%{name}-generate-deps.sh"},
)
