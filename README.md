# Bazel mvn deps
A simple Maven dependency graph generator for Bazel.

Unlike other build systems, Bazel does not provide a dependency management service as part of the build and
does not provide a way to specify a Maven dependency (which will be resolved transitively) and be available during compilation.
<br/>
There are several attempts to solve this problem (such as [sync-deps](https://github.com/spotify/bazel-tools/tree/master/sync-deps), [gmaven](https://github.com/bazelbuild/gmaven_rules), [migration-tooling](https://github.com/bazelbuild/migration-tooling) and [bazel-deps](https://github.com/johnynek/bazel-deps)), but some do not support Kotlin or Android, some do not support customized Maven repositories, etc.
<br/>
This WORKSPACE will provide `deps_workspace_generator_rule` rule which allows you to create a set of rules which can be used as dependencies based on a given list of Maven dependencies. The rule will output the dependencies-graph to a file (similar to Yarn's lock-file).

## Features

* Transitively resolves all dependencies from a given list of Maven dependencies.
* Generates repository-rules for all remote artifacts.
* Generates required Java rule (with transitive dependencies).
* Automatically detects which rule-type to create for a given dependency:
  * `aar_import` for Android artifacts.
  * `java_plugin` for annotation-processors.
  * `kt_jvm_import` for Kotlin modules.
  * `java_import` for anything else.
* Allow to specify custom Maven repo URLs.
* Produces a _lock_ file that describes the dependency graph. This file should be checked into your repo.
  
## Prior-work
The resolving of the Maven dependency graph is done using a modified version of [migration-tooling](https://github.com/bazelbuild/migration-tooling).

## Example

### WORKSPACE file
Note: You might need to also import `http_archive` rules into your workspace: `load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file", "http_archive")`

Add this repository to your WORKSPACE (set `bazel_mvn_deps_version` to the latest [commit](https://github.com/menny/bazel-mvn-deps/commits/master)):
```
bazel_mvn_deps_version = "fcc00b0fef92aa7d1f3935ee2e390ec4c3d5101f"
http_archive(
    name = "bazel_mvn_deps_rule",
    urls = ["https://github.com/menny/bazel-mvn-deps/archive/%s.zip" % bazel_mvn_deps_version],
    type = "zip",
    strip_prefix = "bazel-mvn-deps-%s" % bazel_mvn_deps_version
)

load("@bazel_mvn_deps_rule//others/migration-tooling:dependencies.bzl", "generate_migration_tools_workspace_rules")
generate_migration_tools_workspace_rules()
```

### target definition
In your module's `BUILD.bazel` file (let's say `others/migration-tooling/BUILD.bazel`) load the dependencies rule:
```
load("@bazel_mvn_deps_rule//rules/maven_deps:maven_deps_workspace_generator.bzl", "deps_workspace_generator_rule")
```
And define a target for resolving dependencies:
```
deps_workspace_generator_rule(name = 'main_deps',
    maven_deps = [
        "com.google.guava:guava:20.0",
        "org.apache.commons:commons-lang3:jar:3.8.1",
        "com.google.code.findbugs:jsr305:3.0.2",
        "com.google.auto.value:auto-value:1.6.3"
    ],
    output_deps_file_path = 'others/migration-tooling/dependencies.bzl',
    rule_prefix = 'mvn_main',
    macro_prefix = 'migration_tools')
```
In this example above we defined the target `//others/migration-tooling:main_deps` with 4 maven dependencies:

* `com.google.guava:guava:20.0`
* `org.apache.commons:commons-lang3:jar:3.8.1`
* `com.google.code.findbugs:jsr305:3.0.2`
* `com.google.auto.value:auto-value:1.6.3` - which is an annotation-processor. More on that later in the document.

### Resolving the dependency graph
To generate the transitive rules for the required `maven_deps`, you'll run the target:
```
bazel run //others/migration-tooling:main_deps
```

This will retrieve all the transitive dependencies and resolve conflicts. We will store the resolved dependencies graph (Bazel rules) in the file `others/migration-tooling/dependencies.bzl`. The generated rules will have a prefix `mvn_main` and the generated macros will have the prefix `migration_tools`. These prefixes allows you to generate several graphs for different cases (for example, compile vs annotation-processor stages). This file will need to be checked into your repository, same as [Yarn's lock file](https://yarnpkg.com/lang/en/docs/yarn-lock/).<br/>

### Using the generated Maven dependencies
In modules you which to use those dependencies, first load the generated transitive rules in your module's `BUILD.bazel` file:
```
load("//others/migration-tooling:dependencies.bzl", "generate_migration_tools_transitive_dependency_rules")
generate_migration_tools_transitive_dependency_rules()
```

This will make the rules available in any target defined in that `BUILD.bazel` file as `//others/migration-tooling:mvn_main___XXX`:
* `com.google.guava:guava:20.0` as `//others/migration-tooling:mvn_main___com_google_guava__guava`
* `org.apache.commons:commons-lang3:jar:3.8.1` as `//others/migration-tooling:mvn_main___com_google_code_findbugs__jsr305`
* `com.google.code.findbugs:jsr305:3.0.2` as `//others/migration-tooling:mvn_main___org_apache_commons__commons_lang3`

#### Annotation-Processors

For dependencies that are detected as annotation-processors we are creating a [`java_plugin`](https://docs.bazel.build/versions/master/be/java.html#java_plugin) rule for each detected
[`processor_class`](https://docs.bazel.build/versions/master/be/java.html#java_plugin.processor_class), and then wrap all of these rules in a `java_library` rule that
[exports](https://docs.bazel.build/versions/master/be/java.html#java_library.exported_plugins) the plugins.<br/>
In the example above we included `com.google.auto.value:auto-value:1.6.3`, which is a Java annotation-processor, we create the following rules:
* `//others/migration-tooling:mvn_main___com_google_auto_value__auto_value` - which does not generate API.
* `//others/migration-tooling:mvn_main___com_google_auto_value__auto_value_generate_api` - which _does_ [generate API](https://docs.bazel.build/versions/master/be/java.html#java_plugin.generates_api).

Please, read the [Bazel docs](https://docs.bazel.build/versions/master/be/java.html#java_plugin.generates_api) about which variant you want.<br/>
Also, since we are wrapping the `java_plugin` rules in a `java_library` rules, you should add them to the `deps` list of your rule, and not to the `plugins` list.
