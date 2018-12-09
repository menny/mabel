# Bazel mvn deps
A simple Maven dependency graph generator for Bazel.

Unlike other build systems, Bazel does not provide a dependency management service as part of the build and
does not provide a way to specify a Maven dependency (which will be resolved transitively) and be available during compilation.
<br/>
There are several attempts to solve this problem (such as [sync-deps](https://github.com/spotify/bazel-tools/tree/master/sync-deps), [gmaven](https://github.com/bazelbuild/gmaven_rules), [migration-tooling](https://github.com/bazelbuild/migration-tooling) and [bazel-deps](https://github.com/johnynek/bazel-deps)), but some do not support Kotlin or Android, some do not support customized Maven repositories, etc.
<br/>
This WORKSPACE will provide `deps_workspace_generator_rule` rule which allows you to create a set of rules which can be used as dependencies based on a given list of Maven dependencies. The rule will output the dependencies-graph to a file (similar to Yarn's lock-file).

## Prior-work
The resolving of the Maven dependency graph is done using a modified version of [migration-tooling](https://github.com/bazelbuild/migration-tooling).

## Example
Add this repository to your WORKSPACE:
```
bazel_mvn_deps_version = "92a3cfc08f7f7551ddc1bf92cdc6defb1080b5ff"
http_archive(
    name = "bazel_mvn_deps_rule",
    urls = ["https://github.com/menny/bazel-mvn-deps/archive/%s.zip" % bazel_mvn_deps_version],
    type = "zip",
    strip_prefix = "bazel-mvn-deps-%s" % bazel_mvn_deps_version
)
```

You might need to also import `http_archive` rules into your workspace: `load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file", "http_archive")`
<br/>

