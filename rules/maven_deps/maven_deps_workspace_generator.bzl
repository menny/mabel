
script_template = """
OUTPUT_FILENAME=${{BUILD_WORKING_DIRECTORY}}/{output_deps_file_path}
echo "BUILD_WORKING_DIRECTORY: ${{BUILD_WORKING_DIRECTORY}}"
echo "output_deps_file_path: {output_deps_file_path}"
echo "output_target_build_files_base_path: {output_target_build_files_base_path}"
echo "package_path: {package_path}"
java -jar {resolver} {repositories_list} {artifacts_list} {exclude_artifacts_list} \
    --output_macro_file_path=${{OUTPUT_FILENAME}} \
    --output_target_build_files_base_path=${{BUILD_WORKING_DIRECTORY}}/{output_target_build_files_base_path} \
    --package_path={package_path}
echo "Stored resolved dependencies graph (rules) at ${{OUTPUT_FILENAME}}"
"""

def _impl(ctx):
    output_filename = '{}/{}/dependencies.bzl'.format(ctx.label.package, ctx.label.name)
    output_target_build_files_base_path = '{}/{}/'.format(ctx.label.package, ctx.label.name) if ctx.attr.generate_deps_sub_folder else ""
    package_path = ctx.label.package
    script = ctx.actions.declare_file('%s-generate-deps.sh' % ctx.label.name)
    script_content = script_template.format(
        resolver = ctx.executable._resolver.short_path,
        repositories_list = " ".join(['--repository={}'.format(repository) for repository in ctx.attr.repositories]),
        artifacts_list = " ".join(['--artifact={}'.format(artifact) for artifact in ctx.attr.maven_deps]),
        exclude_artifacts_list = " ".join(['--blacklist={}'.format(exclude_artifact_list) for exclude_artifact_list in ctx.attr.maven_exclude_deps]),
        output_deps_file_path = output_filename,
        output_target_build_files_base_path = output_target_build_files_base_path,
        package_path = package_path,
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
        "maven_deps": attr.string_list(mandatory=True, allow_empty=False, doc = "List of Maven dependencies in the format of `group-id:artifact-id:version`."),
        "maven_exclude_deps": attr.string_list(allow_empty=True, default = [], doc = "List of Maven dependencies which should not be resolved. You can omit the `version` or both `artifact-id:version`."),
        "repositories": attr.string_list(allow_empty=False, default = [
            'https://maven.google.com/',
            'https://repo1.maven.org/maven2/',
            'https://jcenter.bintray.com/',
        ], doc = "List of URLs that point to Maven servers. Defaut is Google, Maven-Central and jcenter."),
        "generate_deps_sub_folder": attr.bool(default=True, doc='If set to True (the default), will create sub-folders with BUILD.bazel file for each dependency.', mandatory=False),
        "_resolver": attr.label(executable=True, allow_files=True, single_file=True, cfg="host", default=Label("//resolver:resolver_deploy.jar"))
    },
    outputs={"out": "%{name}-generate-deps.sh"})
