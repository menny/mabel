package net.evendanan.bazel.mvn.impl;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.License;
import net.evendanan.bazel.mvn.api.Target;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.evendanan.bazel.mvn.TestUtils.createDependency;

public class FormattersTests {

    private static final String HTTP_FILE_TEXT = " http_file(name = 'aar__lib__',\n" +
            "     urls = ['https://some_url/ss.aar'],\n" +
            "     downloaded_file_path = 'ss.aar',\n" +
            " )\n" +
            "\n";
    private static final String JAVA_IMPORT_TEXT = " java_import_impl(name = 'java__lib__',\n" +
            "     jars = ['@java__lib__//file'],\n" +
            "     tags = ['maven_coordinates=java:lib:'],\n" +
            "     licenses = [],\n" +
            "     deps = [\n" +
            "         ':safe_mvn__dep1',\n" +
            "         ':safe_mvn__dep2',\n" +
            "     ],\n" +
            "     exports = [\n" +
            "         ':safe_mvn__export1',\n" +
            "         ':safe_mvn__export2',\n" +
            "     ],\n" +
            "     runtime_deps = [\n" +
            "         ':safe_mvn__runtime1',\n" +
            "         ':safe_mvn__runtime2',\n" +
            "     ],\n" +
            " )\n" +
            "\n" +
            " native.alias(name = 'java__lib',\n" +
            "     actual = ':java__lib__',\n" +
            "     visibility = ['//visibility:public'],\n" +
            " )\n" +
            "\n";
    private static final String JAVA_IMPORT_TEXT_WITH_SOURCES = " java_import_impl(name = 'java__lib__',\n" +
            "     jars = ['@java__lib__//file'],\n" +
            "     tags = ['maven_coordinates=java:lib:'],\n" +
            "     licenses = [],\n" +
            "     deps = [\n" +
            "         ':safe_mvn__dep1',\n" +
            "         ':safe_mvn__dep2',\n" +
            "     ],\n" +
            "     exports = [\n" +
            "         ':safe_mvn__export1',\n" +
            "         ':safe_mvn__export2',\n" +
            "     ],\n" +
            "     runtime_deps = [\n" +
            "         ':safe_mvn__runtime1',\n" +
            "         ':safe_mvn__runtime2',\n" +
            "     ],\n" +
            "     srcjar = '@java__lib____sources//file',\n" +
            " )\n" +
            "\n" +
            " native.alias(name = 'java__lib',\n" +
            "     actual = ':java__lib__',\n" +
            "     visibility = ['//visibility:public'],\n" +
            " )\n" +
            "\n";
    private static final String POM_ONLY_NATIVE_IMPORT_TEXT = " java_import_impl(name = 'parent__lib__',\n" +
            "     jars = [],\n" +
            "     tags = ['maven_coordinates=parent:lib:'],\n" +
            "     licenses = [],\n" +
            "     deps = [\n" +
            "         ':safe_mvn__dep1',\n" +
            "         ':safe_mvn__dep2',\n" +
            "     ],\n" +
            "     exports = [\n" +
            "         ':safe_mvn__export1',\n" +
            "         ':safe_mvn__export2',\n" +
            "     ],\n" +
            "     runtime_deps = [\n" +
            "         ':safe_mvn__runtime1',\n" +
            "         ':safe_mvn__runtime2',\n" +
            "     ],\n" +
            " )\n" +
            "\n" +
            " native.alias(name = 'parent__lib',\n" +
            "     actual = ':parent__lib__',\n" +
            "     visibility = ['//visibility:public'],\n" +
            " )\n" +
            "\n";
    private static final String NATIVE_JAVA_IMPORT_TEXT = "    java_import_impl(name = 'java__lib__',\n" +
            "        jars = ['@java__lib__//file'],\n" +
            "        tags = ['maven_coordinates=java:lib:'],\n" +
            "        licenses = [],\n" +
            "        deps = [\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "        exports = [\n" +
            "            ':safe_mvn__export1',\n" +
            "            ':safe_mvn__export2',\n" +
            "        ],\n" +
            "        runtime_deps = [\n" +
            "            ':safe_mvn__runtime1',\n" +
            "            ':safe_mvn__runtime2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'java__lib',\n" +
            "        actual = ':java__lib__',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n";
    private static final String NATIVE_JAVA_IMPORT_WITH_LICENSE_TEXT = "    java_import_impl(name = 'java__lib__',\n" +
            "        jars = ['@java__lib__//file'],\n" +
            "        tags = ['maven_coordinates=java:lib:'],\n" +
            "        licenses = ['notice'],\n" +
            "        deps = [\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "        exports = [\n" +
            "            ':safe_mvn__export1',\n" +
            "            ':safe_mvn__export2',\n" +
            "        ],\n" +
            "        runtime_deps = [\n" +
            "            ':safe_mvn__runtime1',\n" +
            "            ':safe_mvn__runtime2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'java__lib',\n" +
            "        actual = ':java__lib__',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n";
    private static final String AAR_IMPORT_TEXT = " aar_import_impl(name = 'aar__lib__',\n" +
            "     aar = '@aar__lib__//file',\n" +
            "     deps = [\n" +
            "         ':safe_mvn__dep1',\n" +
            "         ':safe_mvn__dep2',\n" +
            "         ':safe_mvn__runtime1',\n" +
            "         ':safe_mvn__runtime2',\n" +
            "     ],\n" +
            "     exports = [\n" +
            "         ':safe_mvn__export1',\n" +
            "         ':safe_mvn__export2',\n" +
            "     ],\n" +
            " )\n" +
            "\n" +
            " native.alias(name = 'aar__lib',\n" +
            "     actual = ':aar__lib__',\n" +
            "     visibility = ['//visibility:public'],\n" +
            " )\n" +
            "\n";
    private static final String NATIVE_AAR_IMPORT_TEXT = "    aar_import_impl(name = 'aar__lib__',\n" +
            "        aar = '@aar__lib__//file',\n" +
            "        deps = [\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "            ':safe_mvn__runtime1',\n" +
            "            ':safe_mvn__runtime2',\n" +
            "        ],\n" +
            "        exports = [\n" +
            "            ':safe_mvn__export1',\n" +
            "            ':safe_mvn__export2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'aar__lib',\n" +
            "        actual = ':aar__lib__',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n";
    private static final String NATIVE_JAVA_PLUGIN_TEXT = "    java_import_impl(name = 'aar__lib__',\n" +
            "        jars = ['@aar__lib__//file'],\n" +
            "        tags = ['maven_coordinates=aar:lib:'],\n" +
            "        licenses = [],\n" +
            "        deps = [\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "        exports = [\n" +
            "            ':safe_mvn__export1',\n" +
            "            ':safe_mvn__export2',\n" +
            "        ],\n" +
            "        runtime_deps = [\n" +
            "            ':safe_mvn__runtime1',\n" +
            "            ':safe_mvn__runtime2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'aar__lib',\n" +
            "        actual = ':aar__lib__',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n" +
            "    native.java_plugin(name = 'aar__lib_____processor_class_0',\n" +
            "        processor_class = 'com.example.Processor',\n" +
            "        generates_api = 0,\n" +
            "        deps = [\n" +
            "            ':aar__lib__',\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'aar__lib___processor_class_0',\n" +
            "        actual = ':aar__lib_____processor_class_0',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n" +
            "    native.java_plugin(name = 'aar__lib_____generates_api___processor_class_0',\n" +
            "        processor_class = 'com.example.Processor',\n" +
            "        generates_api = 1,\n" +
            "        deps = [\n" +
            "            ':aar__lib__',\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'aar__lib___generates_api___processor_class_0',\n" +
            "        actual = ':aar__lib_____generates_api___processor_class_0',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n" +
            "    native.java_plugin(name = 'aar__lib_____processor_class_1',\n" +
            "        processor_class = 'com.example.Processor2',\n" +
            "        generates_api = 0,\n" +
            "        deps = [\n" +
            "            ':aar__lib__',\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'aar__lib___processor_class_1',\n" +
            "        actual = ':aar__lib_____processor_class_1',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n" +
            "    native.java_plugin(name = 'aar__lib_____generates_api___processor_class_1',\n" +
            "        processor_class = 'com.example.Processor2',\n" +
            "        generates_api = 1,\n" +
            "        deps = [\n" +
            "            ':aar__lib__',\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'aar__lib___generates_api___processor_class_1',\n" +
            "        actual = ':aar__lib_____generates_api___processor_class_1',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n" +
            "    native.java_library(name = 'aar__lib_____processor_class_all',\n" +
            "        exported_plugins = [\n" +
            "            ':aar__lib_____processor_class_0',\n" +
            "            ':aar__lib_____processor_class_1',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'aar__lib___processor_class_all',\n" +
            "        actual = ':aar__lib_____processor_class_all',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n" +
            "    native.java_library(name = 'aar__lib_____generates_api___processor_class_all',\n" +
            "        exported_plugins = [\n" +
            "            ':aar__lib_____generates_api___processor_class_0',\n" +
            "            ':aar__lib_____generates_api___processor_class_1',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'aar__lib___generates_api___processor_class_all',\n" +
            "        actual = ':aar__lib_____generates_api___processor_class_all',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n";
    private static final String NATIVE_KOTLIN_IMPORT_TEXT = "    kotlin_jar_support(name = 'kotlin__lib__',\n" +
            "        deps = [\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "        exports = [\n" +
            "            ':safe_mvn__export1',\n" +
            "            ':safe_mvn__export2',\n" +
            "        ],\n" +
            "        runtime_deps = [\n" +
            "            ':safe_mvn__runtime1',\n" +
            "            ':safe_mvn__runtime2',\n" +
            "        ],\n" +
            "        jar = '@kotlin__lib__//file',\n" +
            "        java_import_impl = java_import_impl,\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'kotlin__lib',\n" +
            "        actual = ':kotlin__lib__',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n";

    private static String targetsToString(String indent, List<Target> targets) {
        StringBuilder builder = new StringBuilder();
        targets.forEach(target -> builder
                .append(target.outputString(indent))
                .append(System.lineSeparator()));

        return builder.toString();
    }

    @Test
    public void testPomOnlyArtifact() {
        final String ruleText = targetsToString(" ", TargetsBuilders.JAVA_IMPORT.buildTargets(
                createDependency("parent:lib",
                        "some_url.pom",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2")),
                DependencyTools.DEFAULT));

        Assert.assertEquals(POM_ONLY_NATIVE_IMPORT_TEXT, ruleText);
    }

    @Test
    public void testPomOnlyArtifactHttp() {
        Assert.assertTrue(new TargetsBuilders.HttpTargetsBuilder(false, dep -> URI.create("")).buildTargets(
                createDependency("parent:lib",
                        "some_url.pom",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2")),
                DependencyTools.DEFAULT).isEmpty());
    }

    @Test
    public void testAarFormatter() {
        final String ruleText = targetsToString(" ", TargetsBuilders.AAR_IMPORT.buildTargets(
                createDependency("aar:lib",
                        "some_url",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2")),
                DependencyTools.DEFAULT));

        Assert.assertEquals(AAR_IMPORT_TEXT, ruleText);
    }

    @Test
    public void testHttpFormatter() {
        final String ruleText = targetsToString(" ", new TargetsBuilders.HttpTargetsBuilder(false, dep -> URI.create("")).buildTargets(
                createDependency("aar:lib",
                        "https://some_url/ss.aar",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2")),
                DependencyTools.DEFAULT));

        Assert.assertEquals(HTTP_FILE_TEXT, ruleText);
    }

    @Test
    public void testJavaImport() {
        final String ruleText = targetsToString(" ", TargetsBuilders.JAVA_IMPORT.buildTargets(
                createDependency("java:lib",
                        "https://some_url",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2")),
                DependencyTools.DEFAULT));

        System.out.println("actual:");
        System.out.println(ruleText);
        System.out.println("actual:");
        System.out.println(ruleText);
        Assert.assertEquals(JAVA_IMPORT_TEXT, ruleText);
    }

    @Test
    public void testJavaImportWithSources() {
        final String ruleText = targetsToString(" ", TargetsBuilders.JAVA_IMPORT.buildTargets(
                createDependency("java:lib",
                        "https://some_url",
                        "https://some_url-sources",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2")),
                DependencyTools.DEFAULT));

        Assert.assertEquals(JAVA_IMPORT_TEXT_WITH_SOURCES, ruleText);
    }

    @Test
    public void testNativeAarFormatter() {
        final String ruleText = targetsToString("    ", TargetsBuilders.AAR_IMPORT.buildTargets(
                createDependency("aar:lib",
                        "some_url",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2")),
                DependencyTools.DEFAULT));

        Assert.assertEquals(NATIVE_AAR_IMPORT_TEXT, ruleText);
    }

    @Test
    public void testNativeJavaImport() {
        final String ruleText = targetsToString("    ", TargetsBuilders.JAVA_IMPORT.buildTargets(
                createDependency("java:lib",
                        "https://some_url",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2")),
                DependencyTools.DEFAULT));

        Assert.assertEquals(NATIVE_JAVA_IMPORT_TEXT, ruleText);
    }

    @Test
    public void testNativeJavaImportWithLicenses() {
        Dependency dependency = Dependency.newBuilder(createDependency("java:lib",
                "https://some_url",
                Arrays.asList("dep1", "dep2"),
                Arrays.asList("export1", "export2"),
                Arrays.asList("runtime1", "runtime2")))
                .clearLicenses()
                .addAllLicenses(Collections.singleton(License.notice))
                .build();

        final String ruleText = targetsToString("    ", TargetsBuilders.JAVA_IMPORT.buildTargets(dependency, DependencyTools.DEFAULT));

        Assert.assertEquals(NATIVE_JAVA_IMPORT_WITH_LICENSE_TEXT, ruleText);
    }

    @Test
    public void testNativeJavaPlugin() {
        final String ruleText = targetsToString("    ", new TargetsBuilders.JavaPluginFormatter(Arrays.asList("com.example.Processor", "com.example.Processor2"))
                .buildTargets(
                        createDependency("aar:lib",
                                "https://some_url",
                                Arrays.asList("dep1", "dep2"),
                                Arrays.asList("export1", "export2"),
                                Arrays.asList("runtime1", "runtime2")),
                        DependencyTools.DEFAULT));

        Assert.assertEquals(NATIVE_JAVA_PLUGIN_TEXT, ruleText);
    }

    @Test
    public void testNativeKotlinImport() {
        final String ruleText = targetsToString("    ", TargetsBuilders.KOTLIN_IMPORT.buildTargets(
                createDependency("kotlin:lib",
                        "https://some_url",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2")),
                DependencyTools.DEFAULT));

        Assert.assertEquals(NATIVE_KOTLIN_IMPORT_TEXT, ruleText);
    }
}