package net.evendanan.bazel.mvn;

import static net.evendanan.bazel.mvn.TestUtils.createMockRule;

import com.google.common.base.Charsets;
import com.google.devtools.bazel.workspace.maven.Rule;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WritersTests {

    private List<Rule> rules;

    @Before
    public void setup() {
        rules = Arrays.asList(
            createMockRule(
                "net.evendanan.dep1:artifact:1.2.3",
                "https://example.com/net/evendanan/dep.jar",
                Arrays.asList("dep_1_1", "dep_1_2"),
                Collections.emptyList(),
                Collections.emptyList()),
            createMockRule(
                "net.evendanan.dep2:artifact:2.0",
                "https://example.com/net/evendanan/dep2.jar",
                Arrays.asList("dep_2_1", "dep_2_2"),
                Arrays.asList("ex_dep_2_1", "ex_dep_2_2"),
                Collections.emptyList()));
    }

    @Test
    public void testRepositoryRulesMacroWriter() throws Exception {
        File outputFile = File.createTempFile("testRepositoryRulesMacroWriter", ".bzl");
        RuleWriters.HttpRepoRulesMacroWriter writer = new RuleWriters.HttpRepoRulesMacroWriter(outputFile, "macro_name");
        writer.write(rules);

        String contents = readFileContents(outputFile);

        Assert.assertEquals(REPO_RULES_MACRO_OUTPUT, contents);
    }

    @Test
    public void testTransitiveRulesMacroWriter() throws Exception {
        File outputFile = File.createTempFile("testRepositoryRulesMacroWriter", ".bzl");
        RuleWriters.TransitiveRulesMacroWriter writer = new RuleWriters.TransitiveRulesMacroWriter(outputFile, "macro_name", rule -> new FakeFormatter());
        writer.write(rules);

        String contents = readFileContents(outputFile);

        Assert.assertEquals(TRANSITIVE_TARGETS_MACRO_OUTPUT, contents);
    }

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

    private static class FakeFormatter implements RuleFormatter {

        @Override
        public String formatRule(final String baseIndent, final Rule rule) {
            return String.format(Locale.US, "%srule(name = '%s',\n%s%sattr = 1)\n",
                baseIndent, rule.mavenGeneratedName(), baseIndent, baseIndent);
        }
    }


    private static final String REPO_RULES_MACRO_OUTPUT = "# Loading a drop-in replacement for native.http_file\n"
                                                          + "load('@bazel_tools//tools/build_defs/repo:http.bzl', 'http_file')\n"
                                                          + "\n"
                                                          + "# Repository rules macro to be run in the WORKSPACE file.\n"
                                                          + "def macro_name():\n"
                                                          + "    http_file(name = 'mvn__net.evendanan.dep1:artifact:1.2.3',\n"
                                                          + "        urls = ['https://example.com/net/evendanan/dep.jar'],\n"
                                                          + "        downloaded_file_path = 'dep.jar',\n"
                                                          + "    )\n"
                                                          + "    http_file(name = 'mvn__net.evendanan.dep2:artifact:2.0',\n"
                                                          + "        urls = ['https://example.com/net/evendanan/dep2.jar'],\n"
                                                          + "        downloaded_file_path = 'dep2.jar',\n"
                                                          + "    )\n"
                                                          + "\n";

    private static final String TRANSITIVE_TARGETS_MACRO_OUTPUT = "# Transitive rules macro to be run in the BUILD.bazel file.\n"
                                                                  + "# If you use kt_* rules, you MUST provide the correct rule implementation when call this macro, if you decide\n"
                                                                  + "# not to provide those implementations we'll try to use java_* rules.\n"
                                                                  + "\n"
                                                                  + "def macro_name(kt_jvm_import=None, kt_jvm_library=None):\n"
                                                                  + "    rule(name = 'mvn__net.evendanan.dep1:artifact:1.2.3',\n"
                                                                  + "        attr = 1)\n"
                                                                  + "\n"
                                                                  + "    rule(name = 'mvn__net.evendanan.dep2:artifact:2.0',\n"
                                                                  + "        attr = 1)\n"
                                                                  + "\n"
                                                                  + "\n";
}