package net.evendanan.bazel.mvn;

import static net.evendanan.bazel.mvn.RuleClassifiers.performRemoteJarInspection;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class RuleClassifiersTest {

    @Test
    public void testAarClassifier() {
        final Rule aar = Mockito.mock(Rule.class);
        Mockito.doReturn("aar").when(aar).packaging();
        Assert.assertSame(RuleFormatters.AAR_IMPORT, RuleClassifiers.AAR_IMPORT.classifyRule(aar).orElse(null));
        Assert.assertSame(RuleFormatters.NATIVE_AAR_IMPORT, RuleClassifiers.NATIVE_AAR_IMPORT.classifyRule(aar).orElse(null));

        Mockito.doReturn("jar").when(aar).packaging();
        Assert.assertSame(Optional.empty(), RuleClassifiers.AAR_IMPORT.classifyRule(aar));
        Assert.assertSame(Optional.empty(), RuleClassifiers.NATIVE_AAR_IMPORT.classifyRule(aar));
    }

    @Test
    public void testJarInspector_unknown() throws Exception {
        final ClassLoader classLoader = RuleClassifiersTest.class.getClassLoader();
        Assert.assertSame(Optional.empty(), performRemoteJarInspection(true, classLoader.getResourceAsStream("dataenum-1.0.2.jar")));
        Assert.assertSame(Optional.empty(), performRemoteJarInspection(false, classLoader.getResourceAsStream("dataenum-1.0.2.jar")));
    }

    @Test
    public void testJarInspector_java_plugin() throws Exception {
        final ClassLoader classLoader = RuleClassifiersTest.class.getClassLoader();
        final RuleFormatter processorFormatter = performRemoteJarInspection(false, classLoader.getResourceAsStream("dataenum-processor-1.0.2.jar")).orElse(null);
        Assert.assertTrue(processorFormatter instanceof RuleFormatters.JavaPluginFormatter);
        List<String> processorClasses = ((RuleFormatters.JavaPluginFormatter) processorFormatter).getProcessorClasses();
        Assert.assertFalse(((RuleFormatters.JavaPluginFormatter) processorFormatter).getIsNative());
        Assert.assertEquals(1, processorClasses.size());
        Assert.assertEquals("com.spotify.dataenum.processor.DataEnumProcessor", processorClasses.get(0));
    }

    @Test
    public void testJarInspector_java_plugin_native() throws Exception {
        final ClassLoader classLoader = RuleClassifiersTest.class.getClassLoader();
        final RuleFormatter processorFormatter = performRemoteJarInspection(true, classLoader.getResourceAsStream("dataenum-processor-1.0.2.jar")).orElse(null);
        Assert.assertTrue(processorFormatter instanceof RuleFormatters.JavaPluginFormatter);
        Assert.assertTrue(((RuleFormatters.JavaPluginFormatter) processorFormatter).getIsNative());
        List<String> processorClasses = ((RuleFormatters.JavaPluginFormatter) processorFormatter).getProcessorClasses();
        Assert.assertEquals(1, processorClasses.size());
        Assert.assertEquals("com.spotify.dataenum.processor.DataEnumProcessor", processorClasses.get(0));
    }

    @Test
    public void testJarInspector_kotlin() throws Exception {
        final ClassLoader classLoader = RuleClassifiersTest.class.getClassLoader();
        Assert.assertSame(RuleFormatters.KOTLIN_IMPORT,
            performRemoteJarInspection(false, classLoader.getResourceAsStream("mockk-1.0.jar")).orElse(null));
        Assert.assertSame(RuleFormatters.NATIVE_KOTLIN_IMPORT,
            performRemoteJarInspection(true, classLoader.getResourceAsStream("mockk-1.0.jar")).orElse(null));
    }
}