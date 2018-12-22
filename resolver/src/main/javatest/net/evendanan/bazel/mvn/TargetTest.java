package net.evendanan.bazel.mvn;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class TargetTest {

    @Test
    public void testHappyPath() {
        Target underTest = new Target("test_rule", "target_name");
        underTest.addString("str_attr", "string value");
        underTest.addBoolean("bool_attr", true);
        underTest.addBoolean("bool2_attr", false);
        underTest.addInt("int_attr", 46);
        underTest.addList("list_empty_attr", Collections.emptyList());
        underTest.addList("list_single_attr", Collections.singleton("only_one"));
        underTest.addList("list_multiple_attr", Arrays.asList("one", "two", "three"));

        StringBuilder builder = new StringBuilder();
        underTest.outputTarget("      ", builder);

        Assert.assertEquals(HAPPY_PATH_OUTPUT, builder.toString());
    }

    @Test
    public void testPublic() {
        Target underTest = new Target("public_test_rule", "public_name");
        underTest.addString("str_attr", "string value");
        underTest.setPublicVisibility();

        StringBuilder builder = new StringBuilder();
        underTest.outputTarget(" ", builder);

        Assert.assertEquals(PUBLIC_HAPPY_PATH_OUTPUT, builder.toString());
    }

    @Test
    public void testAttributeOrderKept() {
        Target underTest = new Target("public_test_rule", "public_name");
        underTest.addString("b_str_attr", "string value");
        underTest.addString("c_str_attr", "string value");
        underTest.addString("a_str_attr", "string value");
        underTest.addInt("bb_int_attr", 1);
        underTest.addInt("ab_int_attr", 1);
        underTest.addInt("cc_int_attr", 1);

        StringBuilder builder = new StringBuilder();
        underTest.outputTarget(" ", builder);
        String output = builder.toString();

        Assert.assertTrue(output.indexOf("b_str_attr") < output.indexOf("c_str_attr"));
        Assert.assertTrue(output.indexOf("c_str_attr") < output.indexOf("a_str_attr"));
        Assert.assertTrue(output.indexOf("a_str_attr") < output.indexOf("bb_int_attr"));
        Assert.assertTrue(output.indexOf("bb_int_attr") < output.indexOf("ab_int_attr"));
        Assert.assertTrue(output.indexOf("ab_int_attr") < output.indexOf("cc_int_attr"));
    }

    @Test
    public void testListValuesSorted() {
        Target underTest = new Target("public_test_rule", "public_name");
        underTest.addList("attrs", Arrays.asList("z_value", "aaa_value", "bbb_value", "a_value", "b_value", "dddddd_value"));
        StringBuilder builder = new StringBuilder();
        underTest.outputTarget(" ", builder);
        String output = builder.toString();

        Assert.assertTrue(output.indexOf("a_value") < output.indexOf("aaa_value"));
        Assert.assertTrue(output.indexOf("aaa_value") < output.indexOf("b_value"));
        Assert.assertTrue(output.indexOf("b_value") < output.indexOf("bbb_value"));
        Assert.assertTrue(output.indexOf("bbb_value") < output.indexOf("dddddd_value"));
        Assert.assertTrue(output.indexOf("dddddd_value") < output.indexOf("z_value"));
    }

    private static final String PUBLIC_HAPPY_PATH_OUTPUT = " public_test_rule(name = 'public_name',\n"
                                                           + "     str_attr = 'string value',\n"
                                                           + "     visibility = ['//visibility:public'],\n"
                                                           + " )\n";

    private static final String HAPPY_PATH_OUTPUT = "      test_rule(name = 'target_name',\n"
                                                    + "          str_attr = 'string value',\n"
                                                    + "          bool_attr = True,\n"
                                                    + "          bool2_attr = False,\n"
                                                    + "          int_attr = 46,\n"
                                                    + "          list_empty_attr = [],\n"
                                                    + "          list_single_attr = ['only_one'],\n"
                                                    + "          list_multiple_attr = [\n"
                                                    + "              'one',\n"
                                                    + "              'three',\n"
                                                    + "              'two',\n"
                                                    + "          ],\n"
                                                    + "      )\n";
}