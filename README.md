# Mabel
[![Latest release](https://img.shields.io/github/release/menny/mabel.svg)](https://github.com/menny/mabel/releases) ![](https://github.com/menny/mabel/workflows/CI/badge.svg?event=push&branch=master) [![codecov](https://codecov.io/gh/menny/mabel/branch/master/graph/badge.svg)](https://codecov.io/gh/menny/mabel)

Yet another Maven dependency graph generator for Bazel.

Mabel provides the `mabel_rule` rule and `artifact` macro, which automatically generate a set of targets that can be used as dependencies based on a given list of Maven coordinates. The rule outputs the dependency graph to a lockfile (similar to Yarn's lock file or npm's package-lock.json).

## Features

* Transitively resolves all dependencies from a given list of Maven dependencies and manages version conflicts, ensuring that only one version of each artifact is available in the dependency graph.
* Generates repository rules for all remote artifacts.
* Generates required Java rules with transitive dependencies.
* Allows marking dependencies as `test_only`.
* Automatically detects which rule type to create for a given dependency:
  * `aar_import` for Android artifacts.
  * `java_plugin` + `java_library` for annotation processors. More about this [here](#annotation-processors).
  * [`jvm_import`](rules/jvm_import/jvm_import.bzl) for anything else.
* Allows implementation replacement for `jvm_import` and `aar_import`. These can be replaced with another rule or macro. See `examples/android/program/BUILD.bazel` for an example.
* Supports custom Maven repository URLs and locking dependencies to a specific Maven repository.
* Adds `licenses` data to `jvm_import` rules if a license is declared in the artifact's POM file. Also adds license metadata to the targets' `tags` attribute:
  * `mabel_license_name` - The name of the license as it appears in the `pom.xml` file.
  * `mabel_license_url` - The URL to the license file as it appears in the `pom.xml` file.
  * `mabel_license_detected_type` - The type of license (`Apache`, `MIT`, `GPL`, etc.) as detected by Mabel.
* Adds `srcjar` if sources are available in the Maven repository.
* Handles POM options:
  * Profiles and placeholders.
  * Version specifications.
  * Dependencies that do not have a POM.
  * Exports the Maven coordinate as a tag in the `jvm_import` rule. This can help with Bazel's [pom_file](https://github.com/google/bazel-common/blob/master/tools/maven/pom_file.bzl) rule.
* Calculates `sha256` for each remote artifact.
* Produces a lockfile that describes the dependency graph. This file should be checked into your repository.

## Why

Unlike other build systems, Bazel does not provide a dependency management service as part of the build and does not provide a way to specify a Maven dependency (which will be resolved transitively) and be available during compilation.

There are several attempts to solve this problem (such as [sync-deps](https://github.com/spotify/bazel-tools/tree/master/sync-deps), [gmaven](https://github.com/bazelbuild/gmaven_rules), [rules_jvm_external](https://github.com/bazelbuild/rules_jvm_external), [migration-tooling](https://github.com/bazelbuild/migration-tooling), [maven-rules](https://github.com/jin/rules_maven), and [bazel-deps](https://github.com/johnynek/bazel-deps)), but some do not support Kotlin or Android, and some do not support customized Maven repositories.

## Usage

### Using Bzlmod (Recommended for Bazel 7.0+)

Mabel uses a two-phase approach for dependency management:

1. **Phase 1: Lockfile Generation** - Run `mabel_rule` target to resolve dependencies and generate a JSON lockfile
2. **Phase 2: Module Extension** - Configure the `maven` extension to read the lockfile and create repository rules

This approach ensures reproducible builds and allows you to review dependency changes before committing them.

#### Step 1: Add Mabel to your MODULE.bazel

Add Mabel and rules_java as dependencies:

```python
bazel_dep(name = "mabel", version = "0.31.0")  # Check latest release
bazel_dep(name = "rules_java", version = "7.11.1")
```

#### Step 2: Define Dependencies in BUILD.bazel

In your module's `BUILD.bazel` file (e.g., `//third_party:BUILD.bazel`), load the `mabel_rule` and `artifact` symbols:

```python
load("@mabel//rules/maven_deps:mabel.bzl", "mabel_rule", "artifact")

mabel_rule(
    name = "maven_deps",
    lockfile_path = "third_party/maven_install.json",
    maven_deps = [
        artifact("com.google.guava:guava:33.0.0-jre"),
        artifact("org.apache.commons:commons-lang3:3.14.0"),
        artifact("com.google.code.findbugs:jsr305:3.0.2"),
    ],
)
```

**Attributes:**

* `name` - A unique name for this target.
* `lockfile_path` - Path to the output JSON lockfile (relative to workspace root). This file will be created/updated when you run the target.
* `maven_deps` - List of `artifact()` macro invocations representing Maven coordinates to resolve.
* `generate_deps_sub_folder` - (Optional) Default `True`. Creates sub-folders with `BUILD.bazel` files for each dependency (for WORKSPACE compatibility).
* `version_conflict_resolver` - (Optional) Default `latest_version`. Strategy for resolving version conflicts. Can be `latest_version` or `breadth_first`.
* `calculate_sha` - (Optional) Default `True`. Calculates the `sha256` value of each remote artifact.
* `fetch_srcjar` - (Optional) Default `False`. Also tries to fetch sources jar for each dependency.

The `artifact()` macro accepts:

* `coordinate` - Maven coordinate in the form `group-id:artifact-id:version` (e.g., `"com.google.guava:guava:33.0.0-jre"`).
* `type` - (Optional) Target type to create. Default is `auto`. Can be `jar`, `aar`, `naive`, `processor`, or `auto`.
* `test_only` - (Optional) Marks this dependency as test-only.
* `maven_exclude_deps` - (Optional) List of Maven dependencies to exclude from resolution.
* `repositories` - (Optional) List of URLs pointing to Maven servers. Defaults to Maven Central.

#### Step 3: Generate the Lockfile

Run the `mabel_rule` target to resolve all transitive dependencies and generate the lockfile:

```bash
bazel run //third_party:maven_deps
```

This will:
1. Resolve all transitive dependencies from the specified Maven coordinates
2. Apply version conflict resolution (using the configured strategy)
3. Download and calculate SHA256 hashes for all artifacts
4. Generate `third_party/maven_install.json` containing all resolved dependency metadata

The lockfile is a JSON file with the following structure:

```json
{
  "version": "1.0",
  "artifacts": {
    "com.google.guava:guava:33.0.0-jre": {
      "repo_name": "com_google_guava__guava__33_0_0_jre",
      "url": "https://repo1.maven.org/maven2/...",
      "sha256": "...",
      "dependencies": ["com_google_code_findbugs__jsr305__3_0_2", ...],
      "exports": [...],
      "runtime_deps": [...],
      "test_only": false,
      "target_type": "jar",
      "licenses": [...]
    }
  }
}
```

**Important:** Commit this lockfile to your version control system. It should be treated like `package-lock.json` in npm or `Cargo.lock` in Rust.

#### Step 4: Configure the Module Extension

In your `MODULE.bazel`, configure the `maven` extension to read the lockfile:

```python
maven = use_extension("@mabel//rules/maven_deps:extensions.bzl", "maven")
maven.install(lockfile = "//third_party:maven_install.json")

# Import all the repositories you need
use_repo(maven,
    "com_google_guava__guava__33_0_0_jre",
    "org_apache_commons__commons_lang3__3_14_0",
    "com_google_code_findbugs__jsr305__3_0_2",
)
```

The `maven.install()` tag accepts:

* `lockfile` - Label pointing to the JSON lockfile generated in Step 3.

The `use_repo()` call imports the repository rules created by the extension. Repository names follow the pattern: `{group_id}__{artifact_id}__{version}` where dots, hyphens, and other special characters are replaced with underscores.

**How to find repository names:**

You can find the exact repository names in the lockfile under the `repo_name` field for each artifact, or use this command:

```bash
cat third_party/maven_install.json | grep -o '"repo_name": "[^"]*"' | cut -d'"' -f4 | sort
```

#### Step 5: Use in Your Targets

Reference the dependencies in your Bazel targets by appending `//file` to the repository name:

```python
java_library(
    name = "mylib",
    srcs = ["MyLib.java"],
    deps = [
        "@com_google_guava__guava__33_0_0_jre//file",
        "@org_apache_commons__commons_lang3__3_14_0//file",
    ],
)
```

The `//file` suffix refers to the downloaded JAR file exposed by the `http_file` repository rule.

#### Updating Dependencies

When you need to update dependencies:

1. Modify the `maven_deps` list in your `mabel_rule` (add, remove, or change versions)
2. Run `bazel run //third_party:maven_deps` to regenerate the lockfile
3. Review the changes to `maven_install.json` (use `git diff`)
4. Update the `use_repo()` call in `MODULE.bazel` if you added or removed dependencies
5. Commit the updated lockfile

#### Working with Multiple Dependency Sets

You can define multiple `mabel_rule` targets for different dependency sets (e.g., separate sets for main code, tests, or different modules):

```python
mabel_rule(
    name = "main_deps",
    lockfile_path = "third_party/main_install.json",
    maven_deps = [
        artifact("com.google.guava:guava:33.0.0-jre"),
    ],
)

mabel_rule(
    name = "test_deps",
    lockfile_path = "third_party/test_install.json",
    maven_deps = [
        artifact("junit:junit:4.13.2", test_only = True),
        artifact("org.mockito:mockito-core:5.0.0", test_only = True),
    ],
)
```

Then configure multiple lockfiles in `MODULE.bazel`:

```python
maven = use_extension("@mabel//rules/maven_deps:extensions.bzl", "maven")
maven.install(lockfile = "//third_party:main_install.json")
maven.install(lockfile = "//third_party:test_install.json")
use_repo(maven,
    "com_google_guava__guava__33_0_0_jre",
    "junit__junit__4_13_2",
    "org_mockito__mockito_core__5_0_0",
)
```

### Using WORKSPACE (Legacy - for Bazel < 7.0)

**Note:** WORKSPACE support is deprecated and will be removed in a future release. Please migrate to bzlmod for new projects.

Add this repository to your WORKSPACE (set `mabel_version` to the latest [release](https://github.com/menny/mabel/releases)):

```python
# Set up rules_java (you probably already have this)
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
mabel_version = "0.30.0"
mabel_sha = "c4487134b386be1d9a4b4f48b1bd6fabd77331188e0ae769cdf08cebc39546d0"
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

#### Target Definition

In your module's `BUILD.bazel` file (e.g., `resolver/BUILD.bazel`), load the dependencies rule and `artifact` macro:

```python
load("@mabel//rules/maven_deps:maven_deps_workspace_generator.bzl", "mabel_rule", "artifact")
```

Define a target for resolving dependencies:

```python
mabel_rule(
    name = 'main_deps',
    maven_deps = [
        artifact("com.google.guava:guava:20.0"),
        artifact("org.apache.commons:commons-lang3:jar:3.8.1"),
        artifact("com.google.code.findbugs:jsr305:3.0.2"),
        artifact("com.google.auto.value:auto-value:1.6.3")
    ],
    generated_targets_prefix = "main_deps___"
)
```

In this example, we defined the target `//resolver:main_deps` with 4 Maven dependencies:

* `com.google.guava:guava:20.0`
* `org.apache.commons:commons-lang3:jar:3.8.1` - here we're specifically requesting the `jar` classifier. In most cases, you don't need to do that.
* `com.google.code.findbugs:jsr305:3.0.2`
* `com.google.auto.value:auto-value:1.6.3` - which is an annotation processor.

#### Resolving the Dependency Graph

To generate the transitive rules for the required `maven_deps`, run the target:

```bash
bazel run //resolver:main_deps
```

This will retrieve all transitive dependencies and resolve conflicts. The resolved dependency graph (Bazel rules) will be stored in the file `resolver/main_deps/dependencies.bzl`, and a folder structure matching all the dependencies will be created:

```
resolver/
    main_deps/
        BUILD.bazel
        dependencies.bzl
        com/
            google/
                guava/
                    guava/
                        BUILD.bazel (with alias guava -> //resolver:main_deps___com_google_guava__guava)
                code/
                    findbugs/
                        jsr305/
                            BUILD.bazel (with alias jsr305 -> //resolver:main_deps___com_google_code_findbugs__jsr305)
                auto/
                    value/
                        auto-value/
                            BUILD.bazel (with alias auto-value -> //resolver:main_deps___com_google_auto_value__auto_value)
        org/
            apache/
                commons/
                    commons-lang3/
                        BUILD.bazel (with alias commons-lang3 -> //resolver:main_deps___org_apache_commons__commons_lang3)
```

You'll notice that there's a prefix `main_deps___` on all targets. This prefix allows you to generate several graphs for different cases (for example, compile vs annotation-processor stages). It was added because we specified `generated_targets_prefix = "main_deps___"` in the target definition.

This file needs to be checked into your repository, the same as [Yarn's lock file](https://yarnpkg.com/lang/en/docs/yarn-lock/).

**Note:** If you don't want the rule to generate sub-folders, you can add `generate_deps_sub_folder = False` to your `mabel_rule` target definition.

#### Using the Generated Maven Dependencies

First, register all the repository rules for the remote Maven artifacts. In your `WORKSPACE` file, add:

```python
load("//resolver/main_deps:dependencies.bzl", main_mabel_deps_rules = "generate_workspace_rules")
main_mabel_deps_rules()
```

In the same module where you declared `mabel_rule` (in our example `//resolver`), add to the `BUILD.bazel` file:

```python
load("//resolver/main_deps:dependencies.bzl", main_generate_transitive_dependency_targets = "generate_transitive_dependency_targets")
main_generate_transitive_dependency_targets()
```

This makes the rules available in any target defined in that `BUILD.bazel` file as `//resolver:main_deps___XXX`:

* `com.google.guava:guava:20.0` as `//resolver:main_deps___com_google_guava__guava`
* `org.apache.commons:commons-lang3:jar:3.8.1` as `//resolver:main_deps___org_apache_commons__commons_lang3`
* `com.google.code.findbugs:jsr305:3.0.2` as `//resolver:main_deps___com_google_code_findbugs__jsr305`

Alternatively, you can use the sub-folder structure (IDEs find this easier to auto-complete):

* `com.google.guava:guava:20.0` as `//resolver/main_deps/com/google/guava/guava`
* `org.apache.commons:commons-lang3:jar:3.8.1` as `//resolver/main_deps/org/apache/commons/commons_lang3`
* `com.google.code.findbugs:jsr305:3.0.2` as `//resolver/main_deps/com/google/code/findbugs/jsr305`

## Rule Configuration

### `mabel_rule`

This rule merges the dependencies into one version-conflict-resolved dependency graph, ensuring you do not have conflicting versions of an artifact in your classpath.

**Attributes:**

* `maven_deps` - List of `artifact` targets representing Maven coordinates.
* `lockfile_path` - (Bzlmod only) Path to output JSON lockfile. If set, a lockfile will be generated for use with the bzlmod extension.
* `generate_deps_sub_folder` - Default `True`. Creates sub-folders with `BUILD.bazel` files for each dependency.
* `keep_output_folder` - Default `False`. Deletes the output folder before generating outputs.
* `public_targets_category` - Default `all`. Sets public visibility of resolved targets. Can be: `requested_deps`, `recursive_exports`, `all`.
* `version_conflict_resolver` - Default `latest_version`. Defines the strategy used to resolve version conflicts. Can be: `latest_version`, `breadth_first`.
* `calculate_sha` - Default `True`. Calculates the `sha256` value of each remote artifact.
* `fetch_srcjar` - Default `False`. Also tries to fetch sources jar for each dependency.
* `generated_targets_prefix` - A prefix to add to all generated targets. Default is empty (no prefix). This is useful if you want to generate several unrelated graphs.
* `output_graph_to_file` - If set to `True`, outputs the graph to `dependencies.txt`. Default is `False`.

### `artifact`

This macro declares a Maven dependency to be resolved and imported into your workspace.

**Attributes:**

* `coordinate` - Maven coordinate in the form `group-id:artifact-id:version`.
* `type` - The type of target(s) to create for this artifact. Default is `auto`. Can be `jar`, `aar`, `naive`, `processor`, or `auto`. For more details, see [TargetType.java](resolver/src/main/java/net/evendanan/bazel/mvn/api/model/TargetType.java).
* `test_only` - Marks this dependency to be used in tests only.
* `maven_exclude_deps` - List of Maven dependencies that should not be resolved. You can omit the `version` or both `artifact-id:version`.
* `repositories` - List of URLs that point to Maven servers. The default list includes Maven Central.

### Real Examples

You can find several examples under the `examples/` folder in this repository. These examples are built as part of the CI process, so they represent working use cases.

## Detected Rules

### Android AARs

If the resolved artifact is an `aar` file, Mabel will create an `aar_import` target.

### Annotation Processors

For dependencies detected as annotation processors, Mabel creates a [`java_plugin`](https://docs.bazel.build/versions/master/be/java.html#java_plugin) rule for each detected [`processor_class`](https://docs.bazel.build/versions/master/be/java.html#java_plugin.processor_class), and then wraps all of these rules in a `java_library` rule that [exports](https://docs.bazel.build/versions/master/be/java.html#java_library.exported_plugins) the plugins.

For example, if you include `com.google.auto.value:auto-value:1.6.3`, which is a Java annotation processor, Mabel creates the following rules:

* `//resolver:main_deps___com_google_auto_value__auto_value` - A `java_library` without annotation-processing capabilities.
* `//resolver:main_deps___com_google_auto_value__auto_value___processor_class_0`..4 - A `java_plugin` with annotation-processing capabilities using each detected processor class. There are four of these because there are four processor classes.
* `//resolver:main_deps___com_google_auto_value__auto_value___generates_api___processor_class_0`..4 - The same as above, but with [`generates_api`](https://docs.bazel.build/versions/master/be/java.html#java_plugin.generates_api) enabled.
* `//resolver:main_deps___com_google_auto_value__auto_value___processor_class_all` - A `java_library` that groups all processors that do not generate API.
* `//resolver:main_deps___com_google_auto_value__auto_value___generates_api___processor_class_all` - Same as above, but generates API.

Please read the [Bazel docs](https://docs.bazel.build/versions/master/be/java.html#java_plugin.generates_api) about which variant you want.

Since Mabel wraps the `java_plugin` rules in `java_library` rules, you should add them to the `deps` list of your rule and not to the `plugins` list, unless you're directly using the `X___processor_class_Y` variant, in which case you should use it in the `plugins` field.

## Migrating from WORKSPACE to Bzlmod

If you're currently using Mabel with WORKSPACE and want to migrate to bzlmod:

1. **Add Mabel to MODULE.bazel:**
   ```python
   bazel_dep(name = "mabel", version = "0.31.0")
   bazel_dep(name = "rules_java", version = "7.11.1")
   ```

2. **Add `lockfile_path` to your existing `mabel_rule`:**
   ```python
   mabel_rule(
       name = "main_deps",
       lockfile_path = "resolver/maven_install.json",  # Add this
       maven_deps = [...],
   )
   ```

3. **Generate the lockfile:**
   ```bash
   bazel run //resolver:main_deps
   ```

4. **Configure the module extension in MODULE.bazel:**
   ```python
   maven = use_extension("@mabel//rules/maven_deps:extensions.bzl", "maven")
   maven.install(lockfile = "//resolver:maven_install.json")
   use_repo(maven, /* list all repo names */)
   ```

5. **Update your dependency references:**
   - Change from `//resolver:main_deps___com_google_guava__guava`
   - To `@com_google_guava__guava__27_0_1_jre//file`

6. **Remove WORKSPACE mabel setup** and the `load()` statements for `generate_workspace_rules()` and `generate_transitive_dependency_targets()`.

7. **Test your build** to ensure everything works.

8. **Commit the lockfile** to version control.

## Troubleshooting

### Repository names not found

If you get errors about missing repositories, make sure all repository names are listed in the `use_repo()` call. You can extract all repository names from the lockfile:

```bash
cat third_party/maven_install.json | grep -o '"repo_name": "[^"]*"' | cut -d'"' -f4 | sort
```

### Version conflicts

Mabel resolves version conflicts using the configured strategy (`latest_version` by default). If you encounter issues:

1. Check the generated lockfile to see which versions were selected
2. Try the alternative `breadth_first` resolver strategy
3. Use `maven_exclude_deps` to explicitly exclude problematic transitive dependencies

### Lockfile changes unexpectedly

If the lockfile changes when you haven't modified dependencies:

1. Maven metadata might have been updated (for SNAPSHOT versions)
2. A transitive dependency might have changed
3. Use `calculate_sha = True` (default) to ensure reproducible builds

### Build errors after updating dependencies

After running `bazel run //third_party:maven_deps`:

1. Review the `git diff` of the lockfile to understand what changed
2. Update the `use_repo()` call in MODULE.bazel if repository names changed
3. Run `bazel clean --expunge` if you encounter caching issues

## Contributing

Contributions are welcome! Please see the [CONTRIBUTING.md](CONTRIBUTING.md) file for guidelines.

## License

Apache License 2.0. See [LICENSE](LICENSE) file for details.
