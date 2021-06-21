"""
A provider that allows us to determine if changing the jar's manifest is needed.
"""
stamp_manifest_provider = provider(doc = "Provides the stamp_enabled configuration", fields = ["stamp_enabled"])

def _impl(ctx):
    return stamp_manifest_provider(stamp_enabled = ctx.build_setting_value)

stamp_manifest = rule(
    implementation = _impl,
    build_setting = config.bool(flag = True),
)
