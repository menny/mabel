"""
Bootstrap module extension for mabel's own dependencies.

This is a temporary extension that wraps the existing WORKSPACE-style
dependencies.bzl file to make it work with bzlmod. Once the lockfile
is generated, this will be replaced with the proper lockfile-based extension.
"""

load("//resolver/main_deps:dependencies.bzl", "generate_workspace_rules")

def _bootstrap_extension_impl(mctx):
    """
    Module extension that calls the existing generate_workspace_rules macro.

    This creates all the repository rules for mabel's Maven dependencies
    by delegating to the existing WORKSPACE-style macro.
    """
    # Call the existing workspace rules macro
    # This will create all the http_file repository rules
    generate_workspace_rules()

bootstrap_extension = module_extension(
    implementation = _bootstrap_extension_impl,
)
