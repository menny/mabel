package net.evendanan.bazel.mvn.impl;

import com.google.common.base.Charsets;
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static net.evendanan.bazel.mvn.TestUtils.createDependency;

public class WritersTests {

    private static final String REPO_RULES_MACRO_OUTPUT_WITH_SOURCES =
            "# Loading a drop-in replacement for native.http_file\n"
                    + "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")\n"
                    + "load(\"@none_default_name_mabel//rules/jvm_import:jvm_import.bzl\", \"jvm_import\")\n"
                    + "\n"
                    + "def macro_name(name = \"macro_name\"):\n"
                    + "    \"\"\"\n"
                    + "    Repository rules macro to be run in the WORKSPACE file.\n"
                    + "\n"
                    + "    Args:\n"
                    + "        name: A unique name for this target. No need to specify.\n"
                    + "    \"\"\"\n"
                    + "\n"
                    + "    # from net.evendanan:dep1:1.2.3\n"
                    + "    http_file(\n"
                    + "        name = \"net_evendanan__dep1__1_2_3\",\n"
                    + "        urls = [\"https://maven.central.org/repo/net/evendanan/dep1/dep1-1.2.3.jar\"],\n"
                    + "        downloaded_file_path = \"dep1-1.2.3.jar\",\n"
                    + "    )\n"
                    + "\n"
                    + "    # from net.evendanan:dep1:1.2.3\n"
                    + "    http_file(\n"
                    + "        name = \"net_evendanan__dep1__1_2_3__sources\",\n"
                    + "        urls = [\"https://maven.central.org/repo/net/evendanan/dep1/dep1-1.2.3-sources.jar\"],\n"
                    + "        downloaded_file_path = \"dep1-1.2.3-sources.jar\",\n"
                    + "    )\n";

    private static final String REPO_RULES_MACRO_OUTPUT =
            "# Loading a drop-in replacement for native.http_file\n"
                    + "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")\n"
                    + "load(\"@mabel//rules/jvm_import:jvm_import.bzl\", \"jvm_import\")\n"
                    + "\n"
                    + "def macro_name(name = \"macro_name\"):\n"
                    + "    \"\"\"\n"
                    + "    Repository rules macro to be run in the WORKSPACE file.\n"
                    + "\n"
                    + "    Args:\n"
                    + "        name: A unique name for this target. No need to specify.\n"
                    + "    \"\"\"\n"
                    + "\n"
                    + "    # from net.evendanan.dep1:artifact:1.2.3\n"
                    + "    http_file(\n"
                    + "        name = \"net_evendanan_dep1__artifact__1_2_3\",\n"
                    + "        urls = [\"https://example.com/net/evendanan/dep.jar\"],\n"
                    + "        downloaded_file_path = \"dep.jar\",\n"
                    + "    )\n"
                    + "\n"
                    + "    # from net.evendanan.dep2:artifact:2.0\n"
                    + "    http_file(\n"
                    + "        name = \"net_evendanan_dep2__artifact__2_0\",\n"
                    + "        urls = [\"https://example.com/com/example/dep2.jar\"],\n"
                    + "        downloaded_file_path = \"dep2.jar\",\n"
                    + "    )\n";

    private static final String TRANSITIVE_TARGETS_MACRO_OUTPUT =
            "\n" +
                    "def _no_op_missing_aar_impl(name, **kwargs):\n" +
                    "    \"\"\"\n" +
                    "    This is a help macro for missing concrete rule implementation.\n" +
                    "\n" +
                    "    This will be used in cases when some dependencies require aar_import rule implementation.\n" +
                    "\n" +
                    "    Args:\n" +
                    "        name: A unique name for this target.\n" +
                    "        **kwargs: Anything else. Not used.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    fail(\n" +
                    "        \"Unable to create target {} since it is a aar_import which was not provided. Add argument aar_import when calling macro_name.\"\n" +
                    "            .format(name),\n" +
                    "    )\n" +
                    "\n" +
                    "def macro_name(\n" +
                    "        name = \"macro_name\",\n" +
                    "        java_library = native.java_library,\n" +
                    "        _java_plugin = native.java_plugin,\n" +
                    "        _jvm_import = jvm_import,\n" +
                    "        _aar_import = _no_op_missing_aar_impl):\n" +
                    "    \"\"\"\n" +
                    "    Macro to set up the transitive rules.\n" +
                    "\n" +
                    "    You can provide your own implementation of jvm_import, aar_import, etc. This can be used\n" +
                    "    in cases where you need to shade (or jar_jar or jetify) your jars.\n" +
                    "\n" +
                    "    Args:\n" +
                    "        name: a unique name for this macro. Not needed to specify.\n" +
                    "        java_library: rule implementation for java_library. Defaults to native.java_library.\n" +
                    "        _java_plugin: rule implementation for java_plugin. Defaults to native.java_plugin.\n" +
                    "        _jvm_import: rule implementation for jvm_import. Defaults to jvm_import.\n" +
                    "        _aar_import: rule implementation for aar_import. Required only if you have Android dependencies.\n" +
                    "    \"\"\"\n" +
                    "\n" +
                    "    # from net.evendanan.dep1:artifact:1.2.3\n" +
                    "    # comment one\n" +
                    "    # comment two\n" +
                    "    java_library(\n" +
                    "        name = \"name_name_1\",\n" +
                    "    )\n" +
                    "\n" +
                    "    # from net.evendanan.dep2:artifact:2.0\n" +
                    "    rule(\n" +
                    "        name = \"name_name_2\",\n" +
                    "    )\n";
    private static final String ALIAS_DEP_1 =
            "\"\"\"\n"
                    + "External Maven targets for artifact net.evendanan.dep1:artifact\n"
                    + "\n"
                    + "Auto-generated by https://github.com/menny/mabel\n"
                    + "\"\"\"\n"
                    + "\n"
                    + "alias(\n"
                    + "    name = \"name_name_1\",\n"
                    + "    actual = \"//path/to/bzl:name_name_1\",\n"
                    + "    visibility = [\"//visibility:public\"],\n"
                    + ")\n";
    private static final String ALIAS_DEP_2 =
            "\"\"\"\n"
                    + "External Maven targets for artifact net.evendanan.dep2:artifact\n"
                    + "\n"
                    + "Auto-generated by https://github.com/menny/mabel\n"
                    + "\"\"\"\n"
                    + "\n"
                    + "alias(\n"
                    + "    name = \"name_name_2\",\n"
                    + "    actual = \"//path/to/bzl:name_name_2\",\n"
                    + "    visibility = [\"//visibility:public\"],\n"
                    + ")\n";
    private static final String ALIAS_DEP_2_MULTIPLE =
            "\"\"\"\n"
                    + "External Maven targets for artifact net.evendanan.dep2:artifact\n"
                    + "\n"
                    + "Auto-generated by https://github.com/menny/mabel\n"
                    + "\"\"\"\n"
                    + "\n"
                    + "alias(\n"
                    + "    name = \"name_name_2\",\n"
                    + "    actual = \"//path/to/bzl:name_name_2\",\n"
                    + "    visibility = [\"//visibility:public\"],\n"
                    + ")\n"
                    + "\n"
                    + "alias(\n"
                    + "    name = \"name_name_2_somethingelse\",\n"
                    + "    actual = \"//path/to/bzl:name_name_2_somethingelse\",\n"
                    + "    visibility = [\"//visibility:public\"],\n"
                    + ")\n";
    private static final String ALIAS_DEP_IGNORES_VERSIONS =
            "\"\"\"\n"
                    + "External Maven targets for artifact net.evendanan.dep1:artifact\n"
                    + "\n"
                    + "Auto-generated by https://github.com/menny/mabel\n"
                    + "\"\"\"\n"
                    + "\n"
                    + "alias(\n"
                    + "    name = \"name_name_1\",\n"
                    + "    actual = \"//path/to/bzl:name_name_1\",\n"
                    + "    visibility = [\"//visibility:public\"],\n"
                    + ")\n"
                    + "\n"
                    + "alias(\n"
                    + "    name = \"name_name_2\",\n"
                    + "    actual = \"//path/to/bzl:name_name_2\",\n"
                    + "    visibility = [\"//visibility:public\"],\n"
                    + ")\n";

    private static String readFileContents(final File file) throws Exception {
        final byte[] buffer = new byte[1024];
        StringBuilder builder = new StringBuilder();

        try (FileInputStream inputStream = new FileInputStream(file)) {
            int read = 0;
            while ((read = inputStream.read(buffer)) >= 0) {
                builder.append(new String(buffer, 0, read, Charsets.UTF_8));
            }
        }

        return builder.toString();
    }

    @Test
    public void testRepositoryRulesMacroWriter() throws Exception {
        File outputFile = File.createTempFile("testRepositoryRulesMacroWriter", ".bzl");
        RuleWriters.HttpRepoRulesMacroWriter writer =
                new RuleWriters.HttpRepoRulesMacroWriter(outputFile, "macro_name", "mabel");
        writer.write(
                Arrays.asList(
                        new TargetsBuilders.HttpTargetsBuilder(false, dep -> URI.create(""))
                                .buildTargets(
                                        createDependency(
                                                "net.evendanan.dep1:artifact:1.2.3",
                                                "https://example.com/net/evendanan/dep.jar",
                                                Arrays.asList("dep_1_1", "dep_1_2"),
                                                Collections.emptyList(),
                                                Collections.emptyList()),
                                        DependencyTools.DEFAULT)
                                .get(0),
                        new TargetsBuilders.HttpTargetsBuilder(false, dep -> URI.create(""))
                                .buildTargets(
                                        createDependency(
                                                "net.evendanan.dep2:artifact:2.0",
                                                "https://example.com/com/example/dep2.jar",
                                                Arrays.asList("dep_2_1", "dep_2_2"),
                                                Arrays.asList("ex_dep_2_1", "ex_dep_2_2"),
                                                Collections.emptyList()),
                                        DependencyTools.DEFAULT)
                                .get(0)));

        String contents = readFileContents(outputFile);
        Assert.assertEquals(REPO_RULES_MACRO_OUTPUT, contents);
        Assert.assertFalse(contents.contains("sha256"));
    }

    @Test
    public void testRepositoryRulesMacroWriterWithSources() throws Exception {
        final List<Target> targets =
                new TargetsBuilders.HttpTargetsBuilder(false, dep -> URI.create(""))
                        .buildTargets(
                                Dependency.builder()
                                        .mavenCoordinate(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "dep1", "1.2.3", "jar"))
                                        .url(
                                                "https://maven.central.org/repo/net/evendanan/dep1/dep1-1.2.3.jar")
                                        .sourcesUrl(
                                                "https://maven.central.org/repo/net/evendanan/dep1/dep1-1.2.3-sources.jar")
                                        .build(),
                                DependencyTools.DEFAULT);

        Assert.assertEquals(2, targets.size());

        File outputFile = File.createTempFile("testRepositoryRulesMacroWriter", ".bzl");
        RuleWriters.HttpRepoRulesMacroWriter writer =
                new RuleWriters.HttpRepoRulesMacroWriter(outputFile, "macro_name", "none_default_name_mabel");
        writer.write(targets);

        Assert.assertEquals(REPO_RULES_MACRO_OUTPUT_WITH_SOURCES, readFileContents(outputFile));
    }

    @Test
    public void testRepositoryRulesMacroWriterWithSha() throws Exception {
        final Function<Dependency, URI> dependencyURIFunction =
                dep -> {
                    try {
                        final File file =
                                File.createTempFile(
                                        "testRepositoryRulesMacroWriterWithSha", "test");
                        Files.write(
                                file.toPath(), "whatever".getBytes(), StandardOpenOption.CREATE);
                        return file.toURI();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
        final TargetsBuilders.HttpTargetsBuilder httpTargetsBuilder =
                new TargetsBuilders.HttpTargetsBuilder(true, dependencyURIFunction);

        final List<Target> targets =
                httpTargetsBuilder.buildTargets(
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "dep1", "1.2.3", "jar"))
                                .url(
                                        "https://maven.central.org/repo/net/evendanan/dep1/dep1-1.2.3.jar")
                                .build(),
                        DependencyTools.DEFAULT);

        Assert.assertEquals(1, targets.size());
        Assert.assertTrue(
                targets.get(0)
                        .outputString("")
                        .contains(
                                "sha256 = \"85738f8f9a7f1b04b5329c590ebcb9e425925c6d0984089c43a022de4f19c281\""));
    }

    @Test
    public void testRepositoryRulesMacroWriterWithShaButDoesNotGeneratesIfVersionIsSnapshot()
            throws Exception {
        final Function<Dependency, URI> dependencyURIFunction =
                dep -> {
                    try {
                        final File file =
                                File.createTempFile(
                                        "testRepositoryRulesMacroWriterWithShaButDoesNotGeneratesIfVersionIsSnapshot",
                                        "test");
                        Files.write(
                                file.toPath(), "whatever".getBytes(), StandardOpenOption.CREATE);
                        return file.toURI();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
        final TargetsBuilders.HttpTargetsBuilder httpTargetsBuilder =
                new TargetsBuilders.HttpTargetsBuilder(true, dependencyURIFunction);

        final List<Target> targets =
                httpTargetsBuilder.buildTargets(
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "dep1", "1.2.3-SNAPSHOT", "jar"))
                                .url(
                                        "https://maven.central.org/repo/net/evendanan/dep1/dep1-1.2.3-SNAPSHOT.jar")
                                .build(),
                        DependencyTools.DEFAULT);

        Assert.assertEquals(1, targets.size());
        Assert.assertFalse(targets.get(0).outputString("").contains("sha256"));
    }

    @Test
    public void testTransitiveRulesAliasWriter() throws Exception {
        File baseFolder =
                Files.createTempDirectory("testTransitiveRulesAliasWriter")
                        .toFile()
                        .getAbsoluteFile();
        RuleWriters.TransitiveRulesAliasWriter writer =
                new RuleWriters.TransitiveRulesAliasWriter(baseFolder, "path/to/bzl");
        writer.write(
                Arrays.asList(
                        new Target("net.evendanan.dep1:artifact:1.2.3", "rule", "name_name_1")
                                .setPublicVisibility(),
                        new Target("net.evendanan.dep2:artifact:2.0", "rule", "name_name_2")
                                .setPublicVisibility()));

        Assert.assertEquals(
                ALIAS_DEP_1,
                readFileContents(new File(baseFolder, "net/evendanan/dep1/artifact/BUILD.bazel")));
        Assert.assertEquals(
                ALIAS_DEP_2,
                readFileContents(new File(baseFolder, "net/evendanan/dep2/artifact/BUILD.bazel")));
    }

    @Test
    public void testTransitiveRulesAliasWriterIgnoresVersions() throws Exception {
        File baseFolder =
                Files.createTempDirectory("testTransitiveRulesAliasWriter")
                        .toFile()
                        .getAbsoluteFile();
        RuleWriters.TransitiveRulesAliasWriter writer =
                new RuleWriters.TransitiveRulesAliasWriter(baseFolder, "path/to/bzl");
        writer.write(
                Arrays.asList(
                        new Target("net.evendanan.dep1:artifact:1.2.3", "rule", "name_name_1")
                                .setPublicVisibility(),
                        new Target("net.evendanan.dep1:artifact:2.0", "rule", "name_name_2")
                                .setPublicVisibility()));

        Assert.assertEquals(
                ALIAS_DEP_IGNORES_VERSIONS,
                readFileContents(new File(baseFolder, "net/evendanan/dep1/artifact/BUILD.bazel")));
    }

    @Test
    public void testTransitiveRulesAliasWriterOnlyPublicTargets() throws Exception {
        File baseFolder =
                Files.createTempDirectory("testTransitiveRulesAliasWriter")
                        .toFile()
                        .getAbsoluteFile();
        RuleWriters.TransitiveRulesAliasWriter writer =
                new RuleWriters.TransitiveRulesAliasWriter(baseFolder, "path/to/bzl");
        writer.write(
                Arrays.asList(
                        new Target("net.evendanan.dep1:artifact:1.2.3", "rule", "name_name_1")
                                .setPublicVisibility(),
                        new Target(
                                "net.evendanan.dep3:artifact:1.2.3",
                                "rule",
                                "name_name_3"), // not public - only target
                        new Target("net.evendanan.dep2:artifact:2.0", "rule", "name_name_2")
                                .setPublicVisibility(),
                        new Target(
                                "net.evendanan.dep2:artifact:2.0",
                                "rule",
                                "name_name_2_somethingelse"))); // same folder, but not public

        Assert.assertTrue(new File(baseFolder, "net/evendanan/dep1/artifact/BUILD.bazel").isFile());
        Assert.assertTrue(new File(baseFolder, "net/evendanan/dep2/artifact/BUILD.bazel").isFile());
        // if does not have any public targets, will not create build file
        Assert.assertFalse(
                new File(baseFolder, "net/evendanan/dep3/artifact/BUILD.bazel").isFile());

        Assert.assertEquals(
                ALIAS_DEP_1,
                readFileContents(new File(baseFolder, "net/evendanan/dep1/artifact/BUILD.bazel")));
        Assert.assertEquals(
                ALIAS_DEP_2,
                readFileContents(new File(baseFolder, "net/evendanan/dep2/artifact/BUILD.bazel")));
    }

    @Test
    public void testTransitiveRulesAliasWriterMultiplePublicTargets() throws Exception {
        File baseFolder =
                Files.createTempDirectory("testTransitiveRulesAliasWriter")
                        .toFile()
                        .getAbsoluteFile();
        RuleWriters.TransitiveRulesAliasWriter writer =
                new RuleWriters.TransitiveRulesAliasWriter(baseFolder, "path/to/bzl");
        writer.write(
                Arrays.asList(
                        new Target("net.evendanan.dep2:artifact:2.0", "rule", "name_name_2")
                                .setPublicVisibility(),
                        new Target(
                                        "net.evendanan.dep2:artifact:2.0",
                                        "rule",
                                        "name_name_2_somethingelse")
                                .setPublicVisibility()));
        Assert.assertEquals(
                ALIAS_DEP_2_MULTIPLE,
                readFileContents(new File(baseFolder, "net/evendanan/dep2/artifact/BUILD.bazel")));
    }

    @Test
    public void testTransitiveRulesAliasWriterWhenTargetFolderDoesNotExist() throws Exception {
        File baseFolder =
                new File(
                        Files.createTempDirectory("testTransitiveRulesAliasWriter")
                                .toFile()
                                .getAbsoluteFile(),
                        "main_deps");

        RuleWriters.TransitiveRulesAliasWriter writer =
                new RuleWriters.TransitiveRulesAliasWriter(baseFolder, "path/to/bzl");
        writer.write(
                Arrays.asList(
                        new Target("net.evendanan.dep1:artifact:1.2.3", "rule", "name_name_1")
                                .setPublicVisibility(),
                        new Target("net.evendanan.dep2:artifact:2.0", "rule", "name_name_2")
                                .setPublicVisibility()));

        Assert.assertEquals(
                ALIAS_DEP_1,
                readFileContents(new File(baseFolder, "net/evendanan/dep1/artifact/BUILD.bazel")));
        Assert.assertEquals(
                ALIAS_DEP_2,
                readFileContents(new File(baseFolder, "net/evendanan/dep2/artifact/BUILD.bazel")));
    }

    @Test
    public void testGetFilePathFromUrl() throws Exception {
        Assert.assertEquals(
                "net/evendanan/lib1/",
                RuleWriters.getFilePathFromMavenName("net.evendanan", "lib1"));
        Assert.assertEquals(
                "net/even-danan/lib1/",
                RuleWriters.getFilePathFromMavenName("net.even-danan", "lib1"));
        Assert.assertEquals(
                "net/evendanan/lib-dep1/",
                RuleWriters.getFilePathFromMavenName("net.evendanan", "lib-dep1"));
    }

    @Test
    public void testTransitiveRulesMacroWriter() throws Exception {
        File outputFile = File.createTempFile("testRepositoryRulesMacroWriter", ".bzl");
        RuleWriters.TransitiveRulesMacroWriter writer =
                new RuleWriters.TransitiveRulesMacroWriter(outputFile, "macro_name");
        Target targetWithComment = new Target("net.evendanan.dep1:artifact:1.2.3", "java_library", "name_name_1");
        targetWithComment.addComment("comment one");
        targetWithComment.addComment("comment two");
        Target targetWithoutComment = new Target("net.evendanan.dep2:artifact:2.0", "rule", "name_name_2");
        writer.write(Arrays.asList(targetWithComment, targetWithoutComment));

        Assert.assertEquals(TRANSITIVE_TARGETS_MACRO_OUTPUT, readFileContents(outputFile));
    }
}
