
script_template = """
java -jar {resolver} {repositories_list} {artifacts_list} {exclude_artifacts_list} --rule_prefix={rule_prefix} --macro_prefix={macro_prefix}
cp ${{PWD}}/generate_workspace.bzl ${{BUILD_WORKING_DIRECTORY}}/{output_deps_file_path}
echo "Stored resolved dependencies graph (rules) at ${{BUILD_WORKING_DIRECTORY}}/{output_deps_file_path}"
"""

def _impl(ctx):
    script = ctx.actions.declare_file('%s-generate-deps.sh' % ctx.label.name)
    script_content = script_template.format(
        resolver = ctx.executable._resolver.short_path,
        repositories_list = " ".join(['--repository={}'.format(repository) for repository in ctx.attr.repositories]),
        artifacts_list = " ".join(['--artifact={}'.format(artifact) for artifact in ctx.attr.maven_deps]),
        exclude_artifacts_list = " ".join(['--blacklist={}'.format(exclude_artifact_list) for exclude_artifact_list in ctx.attr.maven_exclude_deps]),
        output_deps_file_path = ctx.attr.output_deps_file_path,
        rule_prefix = ctx.attr.rule_prefix,
        macro_prefix = ctx.attr.macro_prefix
        )

    ctx.actions.write(script, script_content, is_executable=True)

    return [DefaultInfo(executable=script, runfiles=ctx.runfiles(files = [ctx.executable._resolver]))]

deps_workspace_generator_rule = rule(implementation=_impl, executable=True,
    attrs = {
        "maven_deps": attr.string_list(mandatory=True, allow_empty=False),
        "maven_exclude_deps": attr.string_list(allow_empty=True, default = []),
        "repositories": attr.string_list(allow_empty=False, default = [
            'https://maven.google.com/',
            'https://jcenter.bintray.com/',
            'https://repo1.maven.org/maven2/',
        ]),
        "rule_prefix": attr.string(mandatory=True),
        "macro_prefix": attr.string(mandatory=True),
        "output_deps_file_path": attr.string(mandatory=True),
        "_resolver": attr.label(executable=True, allow_files=True, single_file=True, cfg="host", default=Label("//others/migration-tooling/generate_workspace:resolver_deploy.jar"))
    },
    outputs={"out": "%{name}-generate-deps.sh"})
