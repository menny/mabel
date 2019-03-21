package net.evendanan.bazel.mvn.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class RuleClassifiersTest {

    @Test
    public void testAarClassifier() {
        final Dependency aar = Mockito.mock(Dependency.class);
        Mockito.doReturn("aar").when(aar).packaging();
        Assert.assertSame(TargetsBuilders.AAR_IMPORT, new RuleClassifiers.AarClassifier(false).classifyRule(aar).orElse(null));
        Assert.assertSame(TargetsBuilders.NATIVE_AAR_IMPORT, new RuleClassifiers.AarClassifier(true).classifyRule(aar).orElse(null));

        Mockito.doReturn("jar").when(aar).packaging();
        Assert.assertSame(Optional.empty(), new RuleClassifiers.AarClassifier(true).classifyRule(aar));

        Mockito.doReturn("pom").when(aar).packaging();
        Assert.assertSame(Optional.empty(), new RuleClassifiers.AarClassifier(true).classifyRule(aar));
    }

    @Test
    public void testPomClassifier() {
        final Dependency pom = Mockito.mock(Dependency.class);
        Mockito.doReturn("pom").when(pom).packaging();
        Assert.assertSame(TargetsBuilders.JAVA_IMPORT, new RuleClassifiers.PomClassifier(false).classifyRule(pom).orElse(null));
        Assert.assertSame(TargetsBuilders.NATIVE_JAVA_IMPORT, new RuleClassifiers.PomClassifier(true).classifyRule(pom).orElse(null));

        Mockito.doReturn("jar").when(pom).packaging();
        Assert.assertSame(Optional.empty(), new RuleClassifiers.PomClassifier(true).classifyRule(pom));
    }

    @Test
    public void testJarInspector_unknown() throws Exception {
        final Dependency dependency = Mockito.mock(Dependency.class);
        Mockito.doReturn(URI.create("")).when(dependency).url();
        final Function<Dependency, URI> dependencyURIFunction = dep -> {
            try {
                return RuleClassifiersTest.class.getClassLoader().getResource("dataenum-1.0.2.jar").toURI();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        };
        Assert.assertSame(Optional.empty(), new RuleClassifiers.JarInspector(true, dependencyURIFunction).classifyRule(dependency));
        Assert.assertSame(Optional.empty(), new RuleClassifiers.JarInspector(false, dependencyURIFunction).classifyRule(dependency));
    }

    @Test
    public void testJarInspector_java_plugin() throws Exception {
        final Dependency dependency = Mockito.mock(Dependency.class);
        Mockito.doReturn(URI.create("")).when(dependency).url();
        final Function<Dependency, URI> dependencyURIFunction = dep -> {
            try {
                return RuleClassifiersTest.class.getClassLoader().getResource("dataenum-processor-1.0.2.jar").toURI();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        };
        final TargetsBuilder processorFormatter = new RuleClassifiers.JarInspector(false, dependencyURIFunction).classifyRule(dependency).orElse(null);
        Assert.assertNotNull(processorFormatter);
        Assert.assertTrue(processorFormatter instanceof TargetsBuilders.JavaPluginFormatter);
        List<String> processorClasses = ((TargetsBuilders.JavaPluginFormatter) processorFormatter).getProcessorClasses();
        Assert.assertFalse(((TargetsBuilders.JavaPluginFormatter) processorFormatter).getIsNative());
        Assert.assertEquals(1, processorClasses.size());
        Assert.assertEquals("com.spotify.dataenum.processor.DataEnumProcessor", processorClasses.get(0));
    }

    @Test
    public void testJarInspector_java_plugin_native() throws Exception {
        final Dependency dependency = Mockito.mock(Dependency.class);
        Mockito.doReturn(URI.create("")).when(dependency).url();
        final Function<Dependency, URI> dependencyURIFunction = dep -> {
            try {
                return RuleClassifiersTest.class.getClassLoader().getResource("dataenum-processor-1.0.2.jar").toURI();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        };
        final TargetsBuilder processorFormatter = new RuleClassifiers.JarInspector(true, dependencyURIFunction).classifyRule(dependency).orElse(null);
        Assert.assertNotNull(processorFormatter);
        Assert.assertTrue(processorFormatter instanceof TargetsBuilders.JavaPluginFormatter);
        List<String> processorClasses = ((TargetsBuilders.JavaPluginFormatter) processorFormatter).getProcessorClasses();
        Assert.assertTrue(((TargetsBuilders.JavaPluginFormatter) processorFormatter).getIsNative());
        Assert.assertEquals(1, processorClasses.size());
        Assert.assertEquals("com.spotify.dataenum.processor.DataEnumProcessor", processorClasses.get(0));
    }

    @Test
    public void testJarInspector_kotlin() throws Exception {
        final Dependency dependency = Mockito.mock(Dependency.class);
        Mockito.doReturn(URI.create("")).when(dependency).url();
        final Function<Dependency, URI> dependencyURIFunction = dep -> {
            try {
                return RuleClassifiersTest.class.getClassLoader().getResource("mockk-1.0.jar").toURI();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        };

        Assert.assertSame(TargetsBuilders.KOTLIN_IMPORT, new RuleClassifiers.JarInspector(false, dependencyURIFunction).classifyRule(dependency).orElse(null));
        Assert.assertSame(TargetsBuilders.NATIVE_KOTLIN_IMPORT, new RuleClassifiers.JarInspector(true, dependencyURIFunction).classifyRule(dependency).orElse(null));
    }
}