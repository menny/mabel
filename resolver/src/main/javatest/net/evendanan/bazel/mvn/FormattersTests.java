package net.evendanan.bazel.mvn;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class FormattersTests {

    @Test
    public void testAarFormatter() {
        final String ruleText = RuleFormatters.AAR_IMPORT.formatRule(
            createMockRule("aar_lib",
                "some_url",
                Arrays.asList("dep1", "dep2"),
                Arrays.asList("export1", "export2"),
                Arrays.asList("runtime1", "runtime2")));

        Assert.assertEquals(AAR_IMPORT_TEXT, ruleText);
    }

    @Test
    public void testHttpFormatter() {
        final String ruleText = RuleFormatters.HTTP_FILE.formatRule(
            createMockRule("aar_lib",
                "https://some_url/ss.aar",
                Arrays.asList("dep1", "dep2"),
                Arrays.asList("export1", "export2"),
                Arrays.asList("runtime1", "runtime2")));

        Assert.assertEquals(HTTP_FILE_TEXT, ruleText);
    }

    @Test
    public void testJavaImport() {
        final String ruleText = RuleFormatters.JAVA_IMPORT.formatRule(
            createMockRule("java_lib",
                "https://some_url",
                Arrays.asList("dep1", "dep2"),
                Arrays.asList("export1", "export2"),
                Arrays.asList("runtime1", "runtime2")));

        Assert.assertEquals(JAVA_IMPORT_TEXT, ruleText);
    }

    @Test
    public void testJavaPlugin() {
        final String ruleText = new RuleFormatters.JavaPluginFormatter(Arrays.asList("com.example.Processor", "com.example.Processor2")).formatRule(
            createMockRule("aar_lib",
                "https://some_url",
                Arrays.asList("dep1", "dep2"),
                Arrays.asList("export1", "export2"),
                Arrays.asList("runtime1", "runtime2")));

        Assert.assertEquals(JAVA_PLUGIN_TEXT, ruleText);
    }

    @Test
    public void testKotlinImport() {
        final String ruleText = RuleFormatters.KOTLIN_IMPORT.formatRule(
            createMockRule("kotlin_lib",
                "https://some_url",
                Arrays.asList("dep1", "dep2"),
                Arrays.asList("export1", "export2"),
                Arrays.asList("runtime1", "runtime2")));

        Assert.assertEquals(KOTLIN_IMPORT_TEXT, ruleText);
    }

    private static Rule createMockRule(String mavenDep, String url, List<String> depsLabels, List<String> exportsLabels, List<String> runtimeLabels) {
        final Rule rule = Mockito.mock(Rule.class);

        Mockito.doReturn("mvn__" + mavenDep).when(rule).mavenGeneratedName();
        Mockito.doReturn("safe_mvn__" + mavenDep).when(rule).safeRuleFriendlyName();
        Mockito.doReturn(url).when(rule).getUrl();

        Mockito.doReturn(generateMockedDeps(depsLabels)).when(rule).getDeps();
        Mockito.doReturn(generateMockedDeps(exportsLabels)).when(rule).getExportDeps();
        Mockito.doReturn(generateMockedDeps(runtimeLabels)).when(rule).getRuntimeDeps();

        return rule;
    }

    private static Set<Rule> generateMockedDeps(final List<String> depsLabels) {
        Set<Rule> deps = new HashSet<>(depsLabels.size());
        depsLabels.forEach(dep -> {
            final Rule depRule = Mockito.mock(Rule.class);
            Mockito.doReturn("safe_mvn__" + dep).when(depRule).safeRuleFriendlyName();
            deps.add(depRule);
        });
        return deps;
    }


    private static final String HTTP_FILE_TEXT = "    http_file(name = 'mvn__aar_lib',\n"
                                                 + "        urls = ['https://some_url/ss.aar'],\n"
                                                 + "        downloaded_file_path = 'ss.aar',\n"
                                                 + "    )\n";


    private static final String JAVA_IMPORT_TEXT = "    native.java_import(name = 'mvn__java_lib',\n"
                                                   + "        jars = ['@mvn__java_lib//file'],\n"
                                                   + "        deps = [\n"
                                                   + "            ':safe_mvn__dep1', \n"
                                                   + "            ':safe_mvn__dep2', \n"
                                                   + "        ],\n"
                                                   + "        exports = [\n"
                                                   + "            ':safe_mvn__export1', \n"
                                                   + "            ':safe_mvn__export2', \n"
                                                   + "        ],\n"
                                                   + "        runtime_deps = [\n"
                                                   + "            ':safe_mvn__runtime1', \n"
                                                   + "            ':safe_mvn__runtime2', \n"
                                                   + "        ],\n"
                                                   + "    )\n"
                                                   + "\n"
                                                   + "    native.alias(name = 'safe_mvn__java_lib',\n"
                                                   + "        actual = 'mvn__java_lib',\n"
                                                   + "        visibility = ['//visibility:public'],\n"
                                                   + "    )\n";

    private static final String AAR_IMPORT_TEXT = "    native.aar_import(name = 'mvn__aar_lib',\n"
                                                  + "        aar = '@mvn__aar_lib//file',\n"
                                                  + "        deps = [\n"
                                                  + "            ':safe_mvn__dep1', \n"
                                                  + "            ':safe_mvn__dep2', \n"
                                                  + "            ':safe_mvn__runtime1', \n"
                                                  + "            ':safe_mvn__runtime2', \n"
                                                  + "        ],\n"
                                                  + "        exports = [\n"
                                                  + "            ':safe_mvn__export1', \n"
                                                  + "            ':safe_mvn__export2', \n"
                                                  + "        ],\n"
                                                  + "    )\n"
                                                  + "\n"
                                                  + "    native.alias(name = 'safe_mvn__aar_lib',\n"
                                                  + "        actual = 'mvn__aar_lib',\n"
                                                  + "        visibility = ['//visibility:public'],\n"
                                                  + "    )\n";

    private static final String JAVA_PLUGIN_TEXT = "    native.java_import(name = 'mvn__aar_lib_java_plugin_lib',\n"
                                                   + "        jars = ['@mvn__aar_lib//file'],\n"
                                                   + "        deps = [\n"
                                                   + "            ':safe_mvn__dep1', \n"
                                                   + "            ':safe_mvn__dep2', \n"
                                                   + "        ],\n"
                                                   + "        exports = [\n"
                                                   + "            ':safe_mvn__export1', \n"
                                                   + "            ':safe_mvn__export2', \n"
                                                   + "        ],\n"
                                                   + "        runtime_deps = [\n"
                                                   + "            ':safe_mvn__runtime1', \n"
                                                   + "            ':safe_mvn__runtime2', \n"
                                                   + "        ],\n"
                                                   + "    )\n"
                                                   + "    native.java_plugin(name = 'mvn__aar_lib_0',\n"
                                                   + "        processor_class = 'com.example.Processor',\n"
                                                   + "        generates_api = 0,\n"
                                                   + "        deps = [\n"
                                                   + "            ':mvn__aar_lib_java_plugin_lib', \n"
                                                   + "            ':safe_mvn__dep1', \n"
                                                   + "            ':safe_mvn__dep2', \n"
                                                   + "        ],\n"
                                                   + "    )\n"
                                                   + "    native.java_plugin(name = 'mvn__aar_lib_generate_api_0',\n"
                                                   + "        processor_class = 'com.example.Processor',\n"
                                                   + "        generates_api = 1,\n"
                                                   + "        deps = [\n"
                                                   + "            ':mvn__aar_lib_java_plugin_lib', \n"
                                                   + "            ':safe_mvn__dep1', \n"
                                                   + "            ':safe_mvn__dep2', \n"
                                                   + "        ],\n"
                                                   + "    )\n"
                                                   + "    native.java_plugin(name = 'mvn__aar_lib_1',\n"
                                                   + "        processor_class = 'com.example.Processor2',\n"
                                                   + "        generates_api = 0,\n"
                                                   + "        deps = [\n"
                                                   + "            ':mvn__aar_lib_java_plugin_lib', \n"
                                                   + "            ':safe_mvn__dep1', \n"
                                                   + "            ':safe_mvn__dep2', \n"
                                                   + "        ],\n"
                                                   + "    )\n"
                                                   + "    native.java_plugin(name = 'mvn__aar_lib_generate_api_1',\n"
                                                   + "        processor_class = 'com.example.Processor2',\n"
                                                   + "        generates_api = 1,\n"
                                                   + "        deps = [\n"
                                                   + "            ':mvn__aar_lib_java_plugin_lib', \n"
                                                   + "            ':safe_mvn__dep1', \n"
                                                   + "            ':safe_mvn__dep2', \n"
                                                   + "        ],\n"
                                                   + "    )\n"
                                                   + "    native.java_library(name = 'mvn__aar_lib',\n"
                                                   + "        runtime_deps = [\n"
                                                   + "            ':mvn__aar_lib_java_plugin_lib', \n"
                                                   + "            ':safe_mvn__dep1', \n"
                                                   + "            ':safe_mvn__dep2', \n"
                                                   + "        ],\n"
                                                   + "        exported_plugins = [\n"
                                                   + "            ':mvn__aar_lib_0', \n"
                                                   + "            ':mvn__aar_lib_1', \n"
                                                   + "        ],\n"
                                                   + "    )\n"
                                                   + "    native.java_library(name = 'mvn__aar_lib_generate_api',\n"
                                                   + "        runtime_deps = [\n"
                                                   + "            ':mvn__aar_lib_java_plugin_lib', \n"
                                                   + "            ':safe_mvn__dep1', \n"
                                                   + "            ':safe_mvn__dep2', \n"
                                                   + "        ],\n"
                                                   + "        exported_plugins = [\n"
                                                   + "            ':mvn__aar_lib_generate_api_0', \n"
                                                   + "            ':mvn__aar_lib_generate_api_1', \n"
                                                   + "        ],\n"
                                                   + "    )\n"
                                                   + "\n"
                                                   + "    native.alias(name = 'safe_mvn__aar_lib',\n"
                                                   + "        actual = 'mvn__aar_lib',\n"
                                                   + "        visibility = ['//visibility:public'],\n"
                                                   + "    )\n"
                                                   + "    native.alias(name = 'safe_mvn__aar_lib_generate_api',\n"
                                                   + "        actual = 'mvn__aar_lib_generate_api',\n"
                                                   + "        visibility = ['//visibility:public'],\n"
                                                   + "    )\n";

    private static final String KOTLIN_IMPORT_TEXT = "    kt_jvm_import(name = 'mvn__kotlin_lib_kotlin_jar',\n"
                                                     + "        jars = ['@mvn__kotlin_lib//file'],\n"
                                                     + "    )\n\n"
                                                     + "    kt_jvm_library(name = 'mvn__kotlin_lib',\n"
                                                     + "        jars = ['@mvn__kotlin_lib//file'],\n"
                                                     + "        runtime_deps = [\n"
                                                     + "            ':mvn__kotlin_lib_kotlin_jar', \n"
                                                     + "            ':safe_mvn__dep1', \n"
                                                     + "            ':safe_mvn__dep2', \n"
                                                     + "            ':safe_mvn__runtime1', \n"
                                                     + "            ':safe_mvn__runtime2', \n"
                                                     + "        ],\n"
                                                     + "        exports = [\n"
                                                     + "            ':safe_mvn__export1', \n"
                                                     + "            ':safe_mvn__export2', \n"
                                                     + "        ],\n"
                                                     + "    )\n"
                                                     + "\n"
                                                     + "    native.alias(name = 'safe_mvn__kotlin_lib',\n"
                                                     + "        actual = 'mvn__kotlin_lib',\n"
                                                     + "        visibility = ['//visibility:public'],\n"
                                                     + "    )\n";
}