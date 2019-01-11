
script_resolver_template = """
echo java -jar {resolver} {repositories_list} {artifacts_list} {exclude_artifacts_list} \
    --output_file={output_filename}
"""

TransitiveDataInfo = provider(fields=["graph_file"])

def _impl_resolver(ctx):
    output_filename = ctx.actions.declare_file('%s-transitive-graph.json' % ctx.label.name)
    script_content = script_resolver_template.format(
        resolver = ctx.executable._resolver.short_path,
        repositories_list = " ".join(['--repository={}'.format(repository) for repository in ctx.attr.repositories]),
        artifacts_list = " ".join(['--artifact={}'.format(artifact) for artifact in ctx.attr.maven_deps]),
        exclude_artifacts_list = " ".join(['--blacklist={}'.format(exclude_artifact_list) for exclude_artifact_list in ctx.attr.maven_exclude_deps]),
        output_filename = output_filename,
        )

    ctx.actions.run_shell(outputs=[output_filename],
                          tools=[ctx.executable._resolver],
                          command=script_content,
                          )

    return [TransitiveDataInfo(graph_file=output_filename)]

maven_dependency_graph_rule = rule(implementation=_impl_resolver,
    doc = "Generates a json file that represents this Maven dependency and its transitive dependencies.",
    attrs = {
       "coordinate": attr.string(mandatory=True, allow_empty=False, doc = "Maven coordinate in the form of `group-id:artifact-id:version`."),
       "maven_exclude_deps": attr.string_list(allow_empty=True, default = [], doc = "List of Maven dependencies which should not be resolved. You can omit the `version` or both `artifact-id:version`."),
       "repositories": attr.string_list(allow_empty=False, default = [
           'https://repo1.maven.org/maven2/'
       ], doc = "List of URLs that point to Maven servers. Defaut is Maven-Central."),
       "_resolver": attr.label(executable=True, allow_files=True, single_file=True, cfg="host", default=Label("//resolver:resolver_deploy.jar"))
    },
    outputs={"out": "%{name}-transitive-graph.json"})

script_merger_template = """
echo "BUILD_WORKING_DIRECTORY: '${{BUILD_WORKING_DIRECTORY}}'"
echo "output_filename: '{output_filename}'"
echo "output_target_build_files_base_path: '{output_target_build_files_base_path}'"
echo "package_path: '{package_path}'"
echo "rule_prefix: '{rule_prefix}'"
echo "create_deps_sub_folders: '{create_deps_sub_folders}'"

java -jar {merger} {graph_files_list} \
    --output_macro_file_path={output_filename} \
    --output_target_build_files_base_path=${{BUILD_WORKING_DIRECTORY}}/{output_target_build_files_base_path} \
    --package_path={package_path} \
    --rule_prefix={rule_prefix} \
    --create_deps_sub_folders={create_deps_sub_folders}

echo "Stored resolved dependencies graph (rules) at ${{BUILD_WORKING_DIRECTORY}}/{output_target_build_files_base_path}{output_filename}"
"""

def _impl_merger(ctx):
    output_filename = 'dependencies.bzl'
    output_target_build_files_base_path = '{}/{}/'.format(ctx.label.package, ctx.label.name)
    package_path = ctx.label.package
    source_json_files = [dep[TransitiveDataInfo].graph_file for dep in ctx.attr.maven_deps]
    script = ctx.actions.declare_file('%s-generate-deps.sh' % ctx.label.name)
    script_content = script_merger_template.format(
        merger = ctx.executable._merger.short_path,
        graph_files_list = " ".join(['--graph_file={}'.format(artifact) for artifact in ctx.attr.maven_deps]),
        output_filename = output_filename,
        output_target_build_files_base_path = output_target_build_files_base_path,
        package_path = package_path,
        rule_prefix = "{}___".format(ctx.label.name),
        create_deps_sub_folders = '{}'.format(ctx.attr.generate_deps_sub_folder).lower()
        )

    ctx.actions.write(script, script_content, is_executable=True)

    return [DefaultInfo(executable=script, runfiles=ctx.runfiles(files = [ctx.executable._resolver]))]

deps_workspace_generator_rule = rule(implementation=_impl,
     doc = """Generates a bzl file with repository-rules and targets which describes a Maven dependecy graph based on
     the provided `maven_deps` values. The result will be stored in a `bzl` file in a sub-folder named the same as this rule target's name.
     The generated file will contain two macros:

     * `generate_XXX_workspace_rules` - should be load and run in the `WORKSPACE` file. This will create repository-rules for all the remote Maven artifacts (jars).
     * `generate_XXX_transitive_dependency_rules` - should be loaded in the relavent `BUILD.bazel` file. This will create targets for each of the requested `maven_deps`.

     Additionally, a sub-folder structure will also be generated
     """,
     executable=True,
     attrs = {
         "maven_deps": attr.label_list(
             mandatory=True,
             allow_empty=False,
             allow_files=[".json"],
             providers=[TransitiveDataInfo],
             doc = "List of `maven_dependency_graph_rule` targets."),
         "generate_deps_sub_folder": attr.bool(default=True, doc='If set to True (the default), will create sub-folders with BUILD.bazel file for each dependency.', mandatory=False),
         "_merger": attr.label(executable=True, allow_files=True, single_file=True, cfg="host", default=Label("//resolver:merger_deploy.jar"))
     },
     outputs={"out": "%{name}-generate-deps.sh"})

