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
        Assert.assertSame(TargetsBuilders.AAR_IMPORT, new RuleClassifiers.AarClassifier().classifyRule(aar).orElse(null));

        Mockito.doReturn("jar").when(aar).packaging();
        Assert.assertSame(Optional.empty(), new RuleClassifiers.AarClassifier().classifyRule(aar));

        Mockito.doReturn("pom").when(aar).packaging();
        Assert.assertSame(Optional.empty(), new RuleClassifiers.AarClassifier().classifyRule(aar));
    }

    @Test
    public void testPomClassifier() {
        final Dependency pom = Mockito.mock(Dependency.class);
        Mockito.doReturn("pom").when(pom).packaging();
        Assert.assertSame(TargetsBuilders.JAVA_IMPORT, new RuleClassifiers.PomClassifier().classifyRule(pom).orElse(null));

        Mockito.doReturn("jar").when(pom).packaging();
        Assert.assertSame(Optional.empty(), new RuleClassifiers.PomClassifier().classifyRule(pom));
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
        Assert.assertSame(Optional.empty(), new RuleClassifiers.JarInspector(dependencyURIFunction).classifyRule(dependency));
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
        final TargetsBuilder processorFormatter = new RuleClassifiers.JarInspector(dependencyURIFunction).classifyRule(dependency).orElse(null);
        Assert.assertNotNull(processorFormatter);
        Assert.assertTrue(processorFormatter instanceof TargetsBuilders.JavaPluginFormatter);
        List<String> processorClasses = ((TargetsBuilders.JavaPluginFormatter) processorFormatter).getProcessorClasses();
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
        final TargetsBuilder processorFormatter = new RuleClassifiers.JarInspector(dependencyURIFunction).classifyRule(dependency).orElse(null);
        Assert.assertNotNull(processorFormatter);
        Assert.assertTrue(processorFormatter instanceof TargetsBuilders.JavaPluginFormatter);
        List<String> processorClasses = ((TargetsBuilders.JavaPluginFormatter) processorFormatter).getProcessorClasses();
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

        Assert.assertSame(TargetsBuilders.KOTLIN_IMPORT, new RuleClassifiers.JarInspector(dependencyURIFunction).classifyRule(dependency).orElse(null));
    }
}