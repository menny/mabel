package net.evendanan.bazel.mvn.impl;

import java.util.Arrays;
import java.util.List;
import net.evendanan.bazel.mvn.api.Target;
import org.junit.Assert;
import org.junit.Test;

import static net.evendanan.bazel.mvn.TestUtils.createMockRule;

public class FormattersTests {

    private static final String HTTP_FILE_TEXT = " http_file(name = 'mvn__aar__lib',\n" +
            "     urls = ['https://some_url/ss.aar'],\n" +
            "     downloaded_file_path = 'ss.aar',\n" +
            " )\n" +
            "\n";
    private static final String JAVA_IMPORT_TEXT = " java_import(name = 'mvn__java__lib',\n" +
            "     jars = ['@mvn__java__lib//file'],\n" +
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
            " alias(name = 'safe_mvn__java__lib',\n" +
            "     actual = ':mvn__java__lib',\n" +
            "     visibility = ['//visibility:public'],\n" +
            " )\n" +
            "\n";
    private static final String NATIVE_JAVA_IMPORT_TEXT = "    native.java_import(name = 'mvn__java__lib',\n" +
            "        jars = ['@mvn__java__lib//file'],\n" +
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
            "    native.alias(name = 'safe_mvn__java__lib',\n" +
            "        actual = ':mvn__java__lib',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n";
    private static final String AAR_IMPORT_TEXT = " aar_import(name = 'mvn__aar__lib',\n" +
            "     aar = '@mvn__aar__lib//file',\n" +
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
            " alias(name = 'safe_mvn__aar__lib',\n" +
            "     actual = ':mvn__aar__lib',\n" +
            "     visibility = ['//visibility:public'],\n" +
            " )\n" +
            "\n";
    private static final String NATIVE_AAR_IMPORT_TEXT = "    native.aar_import(name = 'mvn__aar__lib',\n" +
            "        aar = '@mvn__aar__lib//file',\n" +
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
            "    native.alias(name = 'safe_mvn__aar__lib',\n" +
            "        actual = ':mvn__aar__lib',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n";
    private static final String JAVA_PLUGIN_TEXT = " java_import(name = 'mvn__aar__lib_java_plugin_lib',\n" +
            "     jars = ['@mvn__aar__lib//file'],\n" +
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
            " java_plugin(name = 'mvn__aar__lib_0',\n" +
            "     processor_class = 'com.example.Processor',\n" +
            "     generates_api = 0,\n" +
            "     deps = [\n" +
            "         ':mvn__aar__lib_java_plugin_lib',\n" +
            "         ':safe_mvn__dep1',\n" +
            "         ':safe_mvn__dep2',\n" +
            "     ],\n" +
            " )\n" +
            "\n" +
            " java_plugin(name = 'mvn__aar__lib_generate_api_0',\n" +
            "     processor_class = 'com.example.Processor',\n" +
            "     generates_api = 1,\n" +
            "     deps = [\n" +
            "         ':mvn__aar__lib_java_plugin_lib',\n" +
            "         ':safe_mvn__dep1',\n" +
            "         ':safe_mvn__dep2',\n" +
            "     ],\n" +
            " )\n" +
            "\n" +
            " java_plugin(name = 'mvn__aar__lib_1',\n" +
            "     processor_class = 'com.example.Processor2',\n" +
            "     generates_api = 0,\n" +
            "     deps = [\n" +
            "         ':mvn__aar__lib_java_plugin_lib',\n" +
            "         ':safe_mvn__dep1',\n" +
            "         ':safe_mvn__dep2',\n" +
            "     ],\n" +
            " )\n" +
            "\n" +
            " java_plugin(name = 'mvn__aar__lib_generate_api_1',\n" +
            "     processor_class = 'com.example.Processor2',\n" +
            "     generates_api = 1,\n" +
            "     deps = [\n" +
            "         ':mvn__aar__lib_java_plugin_lib',\n" +
            "         ':safe_mvn__dep1',\n" +
            "         ':safe_mvn__dep2',\n" +
            "     ],\n" +
            " )\n" +
            "\n" +
            " java_library(name = 'mvn__aar__lib',\n" +
            "     runtime_deps = [\n" +
            "         ':mvn__aar__lib_java_plugin_lib',\n" +
            "         ':safe_mvn__dep1',\n" +
            "         ':safe_mvn__dep2',\n" +
            "     ],\n" +
            "     exported_plugins = [\n" +
            "         ':mvn__aar__lib_0',\n" +
            "         ':mvn__aar__lib_1',\n" +
            "     ],\n" +
            " )\n" +
            "\n" +
            " java_library(name = 'mvn__aar__lib_generate_api',\n" +
            "     runtime_deps = [\n" +
            "         ':mvn__aar__lib_java_plugin_lib',\n" +
            "         ':safe_mvn__dep1',\n" +
            "         ':safe_mvn__dep2',\n" +
            "     ],\n" +
            "     exported_plugins = [\n" +
            "         ':mvn__aar__lib_generate_api_0',\n" +
            "         ':mvn__aar__lib_generate_api_1',\n" +
            "     ],\n" +
            " )\n" +
            "\n" +
            " alias(name = 'safe_mvn__aar__lib',\n" +
            "     actual = ':mvn__aar__lib',\n" +
            "     visibility = ['//visibility:public'],\n" +
            " )\n" +
            "\n" +
            " alias(name = 'safe_mvn__aar__lib_generate_api',\n" +
            "     actual = ':mvn__aar__lib_generate_api',\n" +
            "     visibility = ['//visibility:public'],\n" +
            " )\n" +
            "\n";
    private static final String NATIVE_JAVA_PLUGIN_TEXT = "    native.java_import(name = 'mvn__aar__lib_java_plugin_lib',\n" +
            "        jars = ['@mvn__aar__lib//file'],\n" +
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
            "    native.java_plugin(name = 'mvn__aar__lib_0',\n" +
            "        processor_class = 'com.example.Processor',\n" +
            "        generates_api = 0,\n" +
            "        deps = [\n" +
            "            ':mvn__aar__lib_java_plugin_lib',\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.java_plugin(name = 'mvn__aar__lib_generate_api_0',\n" +
            "        processor_class = 'com.example.Processor',\n" +
            "        generates_api = 1,\n" +
            "        deps = [\n" +
            "            ':mvn__aar__lib_java_plugin_lib',\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.java_plugin(name = 'mvn__aar__lib_1',\n" +
            "        processor_class = 'com.example.Processor2',\n" +
            "        generates_api = 0,\n" +
            "        deps = [\n" +
            "            ':mvn__aar__lib_java_plugin_lib',\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.java_plugin(name = 'mvn__aar__lib_generate_api_1',\n" +
            "        processor_class = 'com.example.Processor2',\n" +
            "        generates_api = 1,\n" +
            "        deps = [\n" +
            "            ':mvn__aar__lib_java_plugin_lib',\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.java_library(name = 'mvn__aar__lib',\n" +
            "        runtime_deps = [\n" +
            "            ':mvn__aar__lib_java_plugin_lib',\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "        exported_plugins = [\n" +
            "            ':mvn__aar__lib_0',\n" +
            "            ':mvn__aar__lib_1',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.java_library(name = 'mvn__aar__lib_generate_api',\n" +
            "        runtime_deps = [\n" +
            "            ':mvn__aar__lib_java_plugin_lib',\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "        ],\n" +
            "        exported_plugins = [\n" +
            "            ':mvn__aar__lib_generate_api_0',\n" +
            "            ':mvn__aar__lib_generate_api_1',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'safe_mvn__aar__lib',\n" +
            "        actual = ':mvn__aar__lib',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'safe_mvn__aar__lib_generate_api',\n" +
            "        actual = ':mvn__aar__lib_generate_api',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n";
    private static final String NATIVE_KOTLIN_IMPORT_TEXT = "    kt_jvm_import(name = 'mvn__kotlin__lib_kotlin_jar',\n" +
            "        jars = ['@mvn__kotlin__lib//file'],\n" +
            "    )\n" +
            "\n" +
            "    kt_jvm_library(name = 'mvn__kotlin__lib',\n" +
            "        runtime_deps = [\n" +
            "            ':mvn__kotlin__lib_kotlin_jar',\n" +
            "            ':safe_mvn__dep1',\n" +
            "            ':safe_mvn__dep2',\n" +
            "            ':safe_mvn__runtime1',\n" +
            "            ':safe_mvn__runtime2',\n" +
            "        ],\n" +
            "        exports = [\n" +
            "            ':mvn__kotlin__lib_kotlin_jar',\n" +
            "            ':safe_mvn__export1',\n" +
            "            ':safe_mvn__export2',\n" +
            "        ],\n" +
            "    )\n" +
            "\n" +
            "    native.alias(name = 'safe_mvn__kotlin__lib',\n" +
            "        actual = ':mvn__kotlin__lib',\n" +
            "        visibility = ['//visibility:public'],\n" +
            "    )\n" +
            "\n";
    private static final String KOTLIN_IMPORT_TEXT = " kt_jvm_import(name = 'mvn__kotlin__lib_kotlin_jar',\n" +
            "     jars = ['@mvn__kotlin__lib//file'],\n" +
            " )\n" +
            "\n" +
            " kt_jvm_library(name = 'mvn__kotlin__lib',\n" +
            "     runtime_deps = [\n" +
            "         ':mvn__kotlin__lib_kotlin_jar',\n" +
            "         ':safe_mvn__dep1',\n" +
            "         ':safe_mvn__dep2',\n" +
            "         ':safe_mvn__runtime1',\n" +
            "         ':safe_mvn__runtime2',\n" +
            "     ],\n" +
            "     exports = [\n" +
            "         ':mvn__kotlin__lib_kotlin_jar',\n" +
            "         ':safe_mvn__export1',\n" +
            "         ':safe_mvn__export2',\n" +
            "     ],\n" +
            " )\n" +
            "\n" +
            " alias(name = 'safe_mvn__kotlin__lib',\n" +
            "     actual = ':mvn__kotlin__lib',\n" +
            "     visibility = ['//visibility:public'],\n" +
            " )\n" +
            "\n";

    private static String targetsToString(String indent, List<Target> targets) {
        StringBuilder builder = new StringBuilder();
        targets.forEach(target -> {
            target.outputTarget(indent, builder);
            builder.append(System.lineSeparator());
        });

        return builder.toString();
    }

    @Test
    public void testAarFormatter() {
        final String ruleText = targetsToString(" ", TargetsBuilders.AAR_IMPORT.buildTargets(
                createMockRule("aar:lib",
                        "some_url",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2"))));

        Assert.assertEquals(AAR_IMPORT_TEXT, ruleText);
    }

    @Test
    public void testHttpFormatter() {
        final String ruleText = targetsToString(" ", TargetsBuilders.HTTP_FILE.buildTargets(
                createMockRule("aar:lib",
                        "https://some_url/ss.aar",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2"))));

        Assert.assertEquals(HTTP_FILE_TEXT, ruleText);
    }

    @Test
    public void testJavaImport() {
        final String ruleText = targetsToString(" ", TargetsBuilders.JAVA_IMPORT.buildTargets(
                createMockRule("java:lib",
                        "https://some_url",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2"))));

        Assert.assertEquals(JAVA_IMPORT_TEXT, ruleText);
    }

    @Test
    public void testJavaPlugin() {
        final String ruleText = targetsToString(" ", new TargetsBuilders.JavaPluginFormatter(false, Arrays.asList("com.example.Processor", "com.example.Processor2"))
                .buildTargets(createMockRule("aar:lib",
                        "https://some_url",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2"))));

        Assert.assertEquals(JAVA_PLUGIN_TEXT, ruleText);
    }

    @Test
    public void testNativeAarFormatter() {
        final String ruleText = targetsToString("    ", TargetsBuilders.NATIVE_AAR_IMPORT.buildTargets(
                createMockRule("aar:lib",
                        "some_url",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2"))));

        Assert.assertEquals(NATIVE_AAR_IMPORT_TEXT, ruleText);
    }

    @Test
    public void testNativeJavaImport() {
        final String ruleText = targetsToString("    ", TargetsBuilders.NATIVE_JAVA_IMPORT.buildTargets(
                createMockRule("java:lib",
                        "https://some_url",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2"))));

        Assert.assertEquals(NATIVE_JAVA_IMPORT_TEXT, ruleText);
    }

    @Test
    public void testNativeJavaPlugin() {
        final String ruleText = targetsToString("    ", new TargetsBuilders.JavaPluginFormatter(true, Arrays.asList("com.example.Processor", "com.example.Processor2"))
                .buildTargets(
                        createMockRule("aar:lib",
                                "https://some_url",
                                Arrays.asList("dep1", "dep2"),
                                Arrays.asList("export1", "export2"),
                                Arrays.asList("runtime1", "runtime2"))));

        Assert.assertEquals(NATIVE_JAVA_PLUGIN_TEXT, ruleText);
    }

    @Test
    public void testKotlinImport() {
        final String ruleText = targetsToString(" ", TargetsBuilders.KOTLIN_IMPORT.buildTargets(
                createMockRule("kotlin:lib",
                        "https://some_url",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2"))));

        Assert.assertEquals(KOTLIN_IMPORT_TEXT, ruleText);
    }

    @Test
    public void testNativeKotlinImport() {
        final String ruleText = targetsToString("    ", TargetsBuilders.NATIVE_KOTLIN_IMPORT.buildTargets(
                createMockRule("kotlin:lib",
                        "https://some_url",
                        Arrays.asList("dep1", "dep2"),
                        Arrays.asList("export1", "export2"),
                        Arrays.asList("runtime1", "runtime2"))));

        Assert.assertEquals(NATIVE_KOTLIN_IMPORT_TEXT, ruleText);
    }
}