# Bazel mvn deps [![Build Status](https://cloud.drone.io/api/badges/menny/bazel-mvn-deps/status.svg)](https://cloud.drone.io/menny/bazel-mvn-deps)

A simple, extensible, Maven dependency graph generator for Bazel.

Unlike other build systems, Bazel does not provide a dependency management service as part of the build and
does not provide a way to specify a Maven dependency (which will be resolved transitively) and be available during compilation.
<br/>
There are several attempts to solve this problem (such as [sync-deps](https://github.com/spotify/bazel-tools/tree/master/sync-deps), [gmaven](https://github.com/bazelbuild/gmaven_rules), [migration-tooling](https://github.com/bazelbuild/migration-tooling) and [bazel-deps](https://github.com/johnynek/bazel-deps)), but some do not support Kotlin or Android, some do not support customized Maven repositories, etc.
<br/>
<br/>
This WORKSPACE will provide `deps_workspace_generator_rule` rule which allows you to create a set of rules which can be used as dependencies based on a given list of Maven dependencies. The rule will output the dependencies-graph to a file (similar to Yarn's lock-file).

## Features

* Transitively resolves all dependencies from a given list of Maven dependencies.
* Generates repository-rules for all remote artifacts.
* Generates required Java rule (with transitive dependencies).
* Automatically detects which rule-type to create for a given dependency:
  * `aar_import` for Android artifacts.
  * `java_plugin` + `java_library` for annotation-processors. More about this [here](#annotation-processors).
  * `kt_jvm_import` + `kt_jvm_library` or `java_import` for Kotlin modules. More about this [here](#kotlin).
  * `java_import` for anything else.
* Allow to specify custom Maven repo URLs.
* Adds `licenses` data to `java_import` rules, if license is declared in the artifact's POM file.
* Produces a _lock_ file that describes the dependency graph. This file should be checked into your repo.
  
## Prior-work
The resolving of the Maven dependency graph is done using a modified version of [migration-tooling](https://github.com/bazelbuild/migration-tooling).

## Example

### WORKSPACE file
Add this repository to your WORKSPACE (set `bazel_mvn_deps_version` to the latest [commit](https://github.com/menny/bazel-mvn-deps/commits/master)):
```python
bazel_mvn_deps_version = "6bf835b890ddb0600d5a7d1a85e794302bc2de16"
http_archive(
    name = "bazel_mvn_deps_rule",
    urls = ["https://github.com/menny/bazel-mvn-deps/archive/%s.zip" % bazel_mvn_deps_version],
    type = "zip",
    strip_prefix = "bazel-mvn-deps-%s" % bazel_mvn_deps_version
)

load("@bazel_mvn_deps_rule//resolver/main_deps:dependencies.bzl", generate_bazel_mvn_deps_workspace_rules = "generate_workspace_rules")
generate_bazel_mvn_deps_workspace_rules()
```

### Real Examples

You can find a few examples under the `examples/` folder. These examples are built as part of the CI process, so they represent a working use-case.<br/>
*NOTE* - There is an ongoing [issue](https://github.com/menny/bazel-mvn-deps/issues/5) with `kt_jvm_import`. But, Kotlin still works with `java_import`.

### target definition
In your module's `BUILD.bazel` file (let's say `resolver/BUILD.bazel`) load the dependencies rule:
```python
load("@bazel_mvn_deps_rule//rules/maven_deps:maven_deps_workspace_generator.bzl", "deps_workspace_generator_rule")
```
And define a target for resolving dependencies:
```python
deps_workspace_generator_rule(name = 'main_deps',
    maven_deps = [
        "com.google.guava:guava:20.0",
        "org.apache.commons:commons-lang3:jar:3.8.1",
        "com.google.code.findbugs:jsr305:3.0.2",
        "com.google.auto.value:auto-value:1.6.3"
    ])
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

These prefixes allows you to generate several graphs for different cases (for example, compile vs annotation-processor stages). This file will need to be checked into your repository, same as [Yarn's lock file](https://yarnpkg.com/lang/en/docs/yarn-lock/).<br/>
_NOTE:_ If you do not wish the rule to generate the sub-folders, you can add `generate_deps_sub_folder = False` to your `deps_workspace_generator_rule` target definition.
 
### Using the generated Maven dependencies
In modules you which to use those dependencies, first load the generated transitive rules in your module's `BUILD.bazel` file:
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

#### Annotation-Processors

For dependencies that are detected as annotation-processors we are creating a [`java_plugin`](https://docs.bazel.build/versions/master/be/java.html#java_plugin) rule for each detected
[`processor_class`](https://docs.bazel.build/versions/master/be/java.html#java_plugin.processor_class), and then wrap all of these rules in a `java_library` rule that
[exports](https://docs.bazel.build/versions/master/be/java.html#java_library.exported_plugins) the plugins.<br/>
In the example above we included `com.google.auto.value:auto-value:1.6.3`, which is a Java annotation-processor, we create the following rules:
* `//resolver:main_deps___com_google_auto_value__auto_value` - which does not generate API.
* `//resolver:main_deps___com_google_auto_value__auto_value_generate_api` - which _does_ [generate API](https://docs.bazel.build/versions/master/be/java.html#java_plugin.generates_api).

Please, read the [Bazel docs](https://docs.bazel.build/versions/master/be/java.html#java_plugin.generates_api) about which variant you want.<br/>
Also, since we are wrapping the `java_plugin` rules in a `java_library` rules, you should add them to the `deps` list of your rule, and not to the `plugins` list.

#### Kotlin

For [Kotlin](https://github.com/bazelbuild/rules_kotlin), we create a `kt_jvm_import` for each artifact, and then wrap it (along with its deps) in a `kt_jvm_library`. Your rule
depends on the `kt_jvm_library`.<br/>

If your dependencies contain Kotlin rules, you'll need to pass the kt rule-impl to the transitive rules generation macro (in the example above, it is `generate_migration_tools_transitive_dependency_rules`):

```python
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_import", "kt_jvm_library")

load("//resolver/main_deps:dependencies.bzl", main_generate_transitive_dependency_targets = "generate_transitive_dependency_targets")
main_generate_transitive_dependency_targets(kt_jvm_import = kt_jvm_import, kt_jvm_library = kt_jvm_library)
```
<br/>

_Note_: If you decide _not_ to provide `kt_*` implementations, we will try to use `java_import` instead. It should be okay.
<br/>
**NOTE:** Although the mechanism above exists, I couldn't make it work. Either, you know how to fix this, or just use the regular `java_import` (by not supplying `kt_jvm_*`).
<br/>
Another **NOTE**: There is a problem with this, at the moment: `kt_jvm_library` in _master_ does not allow no-source-libraries. So, until the [fix](https://github.com/bazelbuild/rules_kotlin/pull/170) is merged, you can use my branch of the rules:

```python
rules_kotlin_version = "no-src-support"
http_archive(
    name = "io_bazel_rules_kotlin",
    urls = ["https://github.com/menny/rules_kotlin/archive/%s.zip" % rules_kotlin_version],
    type = "zip",
    strip_prefix = "rules_kotlin-%s" % rules_kotlin_version
)

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories()
kt_register_toolchains()
```

