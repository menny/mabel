# Mabel
[![Latest release](https://img.shields.io/github/release/menny/mabel.svg)](https://github.com/menny/mabel/releases) ![](https://github.com/menny/mabel/workflows/CI/badge.svg?event=push&branch=master) [![codecov](https://codecov.io/gh/menny/mabel/branch/master/graph/badge.svg)](https://codecov.io/gh/menny/mabel)

Yet another Maven dependency graph generator for Bazel.

This WORKSPACE will provide `mabel_rule` rule and `artifact` macro which will automatically generate a set of targets that can be used as dependencies based on a given list of Maven coordinates. The rule will output the dependencies-graph to a file (similar to Yarn's lock-file).

## Features

* Transitively resolves all dependencies from a given list of Maven dependencies, and manages version conflicts - ensuring that only one version of each artifact is available in the dependencies graph.
* Generates repository-rules for all remote artifacts.
* Generates required Java rule (with transitive dependencies).
* Allows to mark dependencies as `test_only`.
* Automatically detects which rule-type to create for a given dependency:
  * `aar_import` for Android artifacts.
  * `java_plugin` + `java_library` for annotation-processors. More about this [here](#annotation-processors).
  * [`jvm_import`](rules/jvm_import/jvm_import.bzl) for anything else.
* Allow implementation replacement for `jvm_import` and `aar_import`. Those can be replaced with another rule or macro. See `examples/android/program/BUILD.bazel` for an example.
* Support custom Maven repo URLs and locking dependency for a Maven repository.
* Adds `licenses` data to `jvm_import` rules, if license is declared in the artifact's POM file. Also, adds license metadata to the targets' `tags` attribute:
  * `mabel_license_name` - The name of the license, as appears in the `pom.xml` file.
  * `mabel_license_url` - The URL to the license's file, as appears in the `pom.xml` file.
  * `mabel_license_detected_type` - The type of the license (`Apache`, `MIT`, `GPL`, etc.) as detected by `mabel`. 
* Adds `srcjar` if sources available in the Maven repository.
* Handle POM options:
  * Profiles and placeholders.
  * Version-specification.
  * Dependencies that do not have POM.
  * Exports the Maven coordinate as a tag in the `jvm_import` rule. This can help with Bazel's [pom_file](https://github.com/google/bazel-common/blob/master/tools/maven/pom_file.bzl) rule.
* Calculates `sha256` for each remote artifact.
* Produces a _lock_ file that describes the dependency graph. This file should be checked into your repo.

## Why

Unlike other build systems, Bazel does not provide a dependency management service as part of the build and
does not provide a way to specify a Maven dependency (which will be resolved transitively) and be available during compilation.
<br/>
There are several attempts to solve this problem (such as [sync-deps](https://github.com/spotify/bazel-tools/tree/master/sync-deps), [gmaven](https://github.com/bazelbuild/gmaven_rules), [rules_jvm_external](https://github.com/bazelbuild/rules_jvm_external), [migration-tooling](https://github.com/bazelbuild/migration-tooling), [maven-rules](https://github.com/jin/rules_maven) and [bazel-deps](https://github.com/johnynek/bazel-deps)), but some do not support Kotlin or Android, some do not support customized Maven repositories, etc.

## Example

### WORKSPACE file
Add this repository to your WORKSPACE (set `mabel_version` to the latest [release](https://github.com/menny/mabel/releases) or, if you are adventurous, [commit](https://github.com/menny/mabel/commits/main)):
```python
# We'll need the java_rules already setup, you probably have that already anyway:
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "rules_java",
    urls = [
        "https://github.com/bazelbuild/rules_java/releases/download/5.5.0/rules_java-5.5.0.tar.gz",
    ],
    sha256 = "bcfabfb407cb0c8820141310faa102f7fb92cc806b0f0e26a625196101b0b57e",
)
load("@rules_java//java:repositories.bzl", "rules_java_dependencies", "rules_java_toolchains")
rules_java_dependencies()
rules_java_toolchains()

# Actual mabel setup
# Check out the release page for the latest version
mabel_version = "0.20.0"
mabel_sha = "eeeb41abe0e4199bcbebe6c5f5d8d6221863f67fd140878dfafd5b8d028a7456"
http_archive(
    name = "mabel",
    urls = ["https://github.com/menny/mabel/archive/%s.zip" % mabel_version],
    type = "zip",
    sha256 = mabel_sha,
    strip_prefix = "mabel-%s" % mabel_version
)

load("@mabel//:init_rules.bzl", "init_mabel_rules")
init_mabel_rules()
```

### target definition
In your module's `BUILD.bazel` file (let's say `resolver/BUILD.bazel`) load the dependencies rule and `artifact` macro:
```python
load("@mabel//rules/maven_deps:maven_deps_workspace_generator.bzl", "mabel_rule", "artifact")
```
And define a target for resolving dependencies:
```python
mabel_rule(name = 'main_deps',
    maven_deps = [
        artifact("com.google.guava:guava:20.0"),
        artifact("org.apache.commons:commons-lang3:jar:3.8.1"),
        artifact("com.google.code.findbugs:jsr305:3.0.2"),
        artifact("com.google.auto.value:auto-value:1.6.3")
    ],
    generated_targets_prefix = "main_deps___")
```
In this example above we defined the target `//resolver:main_deps` with 4 maven dependencies:

* `com.google.guava:guava:20.0`
* `org.apache.commons:commons-lang3:jar:3.8.1` - here we are specifically asking for `jar` classifier. In most cases we don't need to do that.
* `com.google.code.findbugs:jsr305:3.0.2`
* `com.google.auto.value:auto-value:1.6.3` - which is an annotation-processor.

### Resolving the dependency graph
To generate the transitive rules for the required `maven_deps`, you'll run the target:
```bash
bazel run //resolver:main_deps
```

This will retrieve all the transitive dependencies and resolve conflicts. We will store the resolved dependencies graph (Bazel rules) in the file `resolver/main_deps/dependencies.bzl`, and will create a folder-structure matching all the deps:
```
resolver/
    main_deps/
        BUILD.bazel
        dependencies.bzl
        com\
            google\
                guava\
                    guave\
                        BUILD.bazel (with alias guava -> //resolver:main_deps___com_google_guava__guave)
                code\
                    findbugs\
                        jsr305\
                            BUILD.bazel (with alias jsr305 -> //resolver:main_deps___com_google_code_findbugs__jsr305)
                auto\
                    value\
                        auto-value\
                            BUILD.bazel (with alias auto-value -> //resolver:main_deps___com_google_auto_value__auto_value)
        org\
            apache\
                commons\
                    commons-lang3\
                        BUILD.bazel (with alias commons-lang3 -> //resolver:main_deps___org_apache_commons__commons_lang3)
```

You'll noticed that there's a prefix `main_deps___` to all targets, this prefix allows you to generate several graphs for different cases (for example, compile vs annotation-processor stages).
It was added because we specified `generated_targets_prefix = "main_deps___"` in the target definition.
<br/>
This file will need to be checked into your repository, same as [Yarn's lock file](https://yarnpkg.com/lang/en/docs/yarn-lock/).<br/>
_NOTE:_ If you do not wish the rule to generate the sub-folders, you can add `generate_deps_sub_folder = False` to your `artifact` target definition.
 
### Using the generated Maven dependencies
First, you'll need to register all the repository rules for the remote maven artifacts. In your `WORKSPACE` file, add:

```python
load("//resolver/main_deps:dependencies.bzl", main_mabel_deps_rules = "generate_workspace_rules")
main_mabel_deps_rules()
```

And, in the same module you declared `mabel_rule` (in our example `//resolver`) add to the `BUILD.bazel` file:
```python
load("//resolver/main_deps:dependencies.bzl", main_generate_transitive_dependency_targets = "generate_transitive_dependency_targets")
main_generate_transitive_dependency_targets()
```

This will make the rules available in any target defined in that `BUILD.bazel` file as `//resolver:mvn_main___XXX`:
* `com.google.guava:guava:20.0` as `//resolver:main_deps___com_google_guava__guava`
* `org.apache.commons:commons-lang3:jar:3.8.1` as `//resolver:main_deps___org_apache_commons__commons_lang3`
* `com.google.code.findbugs:jsr305:3.0.2` as `//resolver:main_deps___com_google_code_findbugs__jsr305`

Or, you can use the sub-folder structure (IDEs find this easier to auto-complete):
* `com.google.guava:guava:20.0` as `//resolver/main_deps/com/google/guava/guava`
* `org.apache.commons:commons-lang3:jar:3.8.1` as `//resolver/main_deps/org/apache/commons/commons_lang3`
* `com.google.code.findbugs:jsr305:3.0.2` as `//resolver/main_deps/com/google/code/findbugs/jsr305`

## Rule configuration

### `mabel_rule`

This rule will merge the dependencies into one, version-conflict-resolved, dependencies graph ensuring you do not have conflicting versions of an artifact in your classpath.</br>
Attributes:

* `maven_deps`: List of `artifact` targets representing a Maven coordinate.
* `generate_deps_sub_folder`: Default `True`. Will create sub-folders with `BUILD.bazel` file for each dependency.
* `keep_output_folder`: Default `False`. Will delete the output folder prior to generating the outputs.
* `public_targets_category`: Default `all`. Set public visibility of resolved targets. Can be: `requested_deps`, `recursive_exports`, `all`.
* `version_conflict_resolver`: Default `latest_version`. Defines the strategy used to resolve version-conflicts. Default is `latest_version`. Can be: `latest_version`, `breadth_first`.
* `calculate_sha`: Default `True`. Will calculate the `sha256` value of each remote artifact.
* `fetch_srcjar`: Default `False`. Will also try to fetch sources jar for each dependency.
* `generated_targets_prefix`: A prefix to add to all generated targets. Default is an empty string, meaning no-prefix. This might be useful if you want to generate several, unrelated, graphs. 
* `output_graph_to_file`: If set to `True`, will output the graph to `dependencies.txt`. Default is `False`.

### `artifact`

This rule declares a Maven dependency to be resolved and import into your WORKSPACE.</br>
Attributes:

* `coordinate`: Maven coordinate in the form of `group-id:artifact-id:version`.
* `type`: What is the type of target(s) to create for this artifact. Default `auto`. Can be `jar`, `aar`, `naive`, `processor`, `auto`. For more details, see [here](resolver/src/main/java/net/evendanan/bazel/mvn/api/model/TargetType.java).
* `test_only`: Mark this dependency to be used in tests only.
* `maven_exclude_deps`: List of Maven dependencies which should not be resolved. You can omit the `version` or both `artifact-id:version`.
* `repositories`: List of URLs that point to Maven servers. The default list includes Maven-Central.

### Real Examples

You can find a few examples under the `examples/` folder in this repo. These examples are built as part of the CI process, so they represent a working use-case.<br/>

## Detected rules

### Android AARs

If the resolved artifact is an `aar` file, we'll create `aar_import` target.

### Annotation-Processors

For dependencies that are detected as annotation-processors we are creating a [`java_plugin`](https://docs.bazel.build/versions/master/be/java.html#java_plugin) rule for each detected
[`processor_class`](https://docs.bazel.build/versions/master/be/java.html#java_plugin.processor_class), and then wrap all of these rules in a `java_library` rule that
[exports](https://docs.bazel.build/versions/master/be/java.html#java_library.exported_plugins) the plugins.<br/>
In the example above we included `com.google.auto.value:auto-value:1.6.3`, which is a Java annotation-processor, we create the following rules:
* `//resolver:main_deps___com_google_auto_value__auto_value` - which is a `java_library` without any annotation-processing capabilities.
* `//resolver:main_deps___com_google_auto_value__auto_value___processor_class_0`..4 - which is a `java_plugin` with annotation-processing capabilities using the first detected processor-class. Four of those, because there are four such classes.
* `//resolver:main_deps___com_google_auto_value__auto_value___generates_api___processor_class_0`..4 - the same as before, but [generate API](https://docs.bazel.build/versions/master/be/java.html#java_plugin.generates_api).
* `//resolver:main_deps___com_google_auto_value__auto_value___processor_class_all` - a `java_library` that groups all the processors that do not generate API.
* `//resolver:main_deps___com_google_auto_value__auto_value___generates_api___processor_class_all` - same as before, but generates API.

Please, read the [Bazel docs](https://docs.bazel.build/versions/master/be/java.html#java_plugin.generates_api) about which variant you want.<br/>
Also, since we are wrapping the `java_plugin` rules in a `java_library` rules, you should add them to the `deps` list of your rule and not to the `plugins` list, unless
you are directly using the `X___processor_class_Y` variant in which case you should use it in the `plugins` field.
