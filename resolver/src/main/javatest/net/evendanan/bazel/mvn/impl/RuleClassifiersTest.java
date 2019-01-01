package net.evendanan.bazel.mvn.impl;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.util.List;
import java.util.Optional;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static net.evendanan.bazel.mvn.impl.RuleClassifiers.performRemoteJarInspection;

public class RuleClassifiersTest {

    @Test
    public void testAarClassifier() {
        final Rule aar = Mockito.mock(Rule.class);
        Mockito.doReturn("aar").when(aar).packaging();
        Assert.assertSame(TargetsBuilders.AAR_IMPORT, RuleClassifiers.AAR_IMPORT.classifyRule(aar).orElse(null));
        Assert.assertSame(TargetsBuilders.NATIVE_AAR_IMPORT, RuleClassifiers.NATIVE_AAR_IMPORT.classifyRule(aar).orElse(null));

        Mockito.doReturn("jar").when(aar).packaging();
        Assert.assertSame(Optional.empty(), RuleClassifiers.AAR_IMPORT.classifyRule(aar));
        Assert.assertSame(Optional.empty(), RuleClassifiers.NATIVE_AAR_IMPORT.classifyRule(aar));
        Assert.assertSame(Optional.empty(), RuleClassifiers.NATIVE_POM_IMPORT.classifyRule(aar));
    }

    @Test
    public void testPomClassifier() {
        final Rule pom = Mockito.mock(Rule.class);
        Mockito.doReturn("pom").when(pom).packaging();
        Assert.assertSame(Optional.empty(), RuleClassifiers.NATIVE_AAR_IMPORT.classifyRule(pom));
        Assert.assertSame(TargetsBuilders.NATIVE_JAVA_IMPORT, RuleClassifiers.NATIVE_POM_IMPORT.classifyRule(pom).orElse(null));
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
        final TargetsBuilder processorFormatter = performRemoteJarInspection(false, classLoader.getResourceAsStream("dataenum-processor-1.0.2.jar")).orElse(null);
        Assert.assertTrue(processorFormatter instanceof TargetsBuilders.JavaPluginFormatter);
        List<String> processorClasses = ((TargetsBuilders.JavaPluginFormatter) processorFormatter).getProcessorClasses();
        Assert.assertFalse(((TargetsBuilders.JavaPluginFormatter) processorFormatter).getIsNative());
        Assert.assertEquals(1, processorClasses.size());
        Assert.assertEquals("com.spotify.dataenum.processor.DataEnumProcessor", processorClasses.get(0));
    }

    @Test
    public void testJarInspector_java_plugin_native() throws Exception {
        final ClassLoader classLoader = RuleClassifiersTest.class.getClassLoader();
        final TargetsBuilder processorFormatter = performRemoteJarInspection(true, classLoader.getResourceAsStream("dataenum-processor-1.0.2.jar")).orElse(null);
        Assert.assertTrue(processorFormatter instanceof TargetsBuilders.JavaPluginFormatter);
        Assert.assertTrue(((TargetsBuilders.JavaPluginFormatter) processorFormatter).getIsNative());
        List<String> processorClasses = ((TargetsBuilders.JavaPluginFormatter) processorFormatter).getProcessorClasses();
        Assert.assertEquals(1, processorClasses.size());
        Assert.assertEquals("com.spotify.dataenum.processor.DataEnumProcessor", processorClasses.get(0));
    }

    @Test
    public void testJarInspector_kotlin() throws Exception {
        final ClassLoader classLoader = RuleClassifiersTest.class.getClassLoader();
        Assert.assertSame(TargetsBuilders.KOTLIN_IMPORT,
                performRemoteJarInspection(false, classLoader.getResourceAsStream("mockk-1.0.jar")).orElse(null));
        Assert.assertSame(TargetsBuilders.NATIVE_KOTLIN_IMPORT,
                performRemoteJarInspection(true, classLoader.getResourceAsStream("mockk-1.0.jar")).orElse(null));
    }
}