package net.evendanan.bazel.mvn.impl;

import com.google.common.io.Resources;

import net.evendanan.bazel.mvn.api.RuleClassifier;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class RuleClassifiersTest {

    private MavenCoordinate mMavenCoordinate;

    @Before
    public void setUp() {
        mMavenCoordinate = MavenCoordinate.create("g", "a", "1", "jar");
    }

    @Test
    public void testAarClassifier() {
        Dependency dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "aar"))
                        .build();
        Assert.assertSame(
                TargetsBuilders.AAR_IMPORT_WITHOUT_EXPORTS,
                new RuleClassifiers.AarClassifier().classifyRule(dep).get(0));

        dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "jar"))
                        .build();
        Assert.assertTrue(new RuleClassifiers.AarClassifier().classifyRule(dep).isEmpty());

        dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "pom"))
                        .build();
        Assert.assertTrue(new RuleClassifiers.AarClassifier().classifyRule(dep).isEmpty());
    }

    @Test
    public void testPomClassifier() {
        Dependency dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "pom"))
                        .build();
        Assert.assertSame(
                TargetsBuilders.JAVA_IMPORT,
                new RuleClassifiers.PomClassifier().classifyRule(dep).get(0));

        dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "jar"))
                        .build();
        Assert.assertTrue(new RuleClassifiers.PomClassifier().classifyRule(dep).isEmpty());
    }

    @Test
    public void testJarInspector_unknown() throws Exception {
        final Dependency dependency =
                Dependency.builder().mavenCoordinate(mMavenCoordinate).build();
        final Function<Dependency, URI> dependencyURIFunction =
                dep -> {
                    try {
                        return RuleClassifiersTest.class
                                .getClassLoader()
                                .getResource("dataenum-1.0.2.jar")
                                .toURI();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                };
        Assert.assertTrue(
                new RuleClassifiers.JarInspector(dependencyURIFunction)
                        .findAllPossibleBuilders(dependency)
                        .isEmpty());
    }

    @Test
    public void testJarInspector_java_plugin() throws Exception {
        final Dependency dependency =
                Dependency.builder().mavenCoordinate(mMavenCoordinate).build();
        final Function<Dependency, URI> dependencyURIFunction =
                dep -> {
                    try {
                        return Resources.getResource("dataenum-processor-1.0.2.jar")
                                .toURI();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                };
        final TargetsBuilder processorFormatter =
                new RuleClassifiers.JarInspector(dependencyURIFunction)
                        .findAllPossibleBuilders(dependency)
                        .stream()
                        .filter(possibleBuilder -> possibleBuilder instanceof TargetsBuilders.JavaPluginFormatter)
                        .findFirst()
                        .orElse(null);
        Assert.assertNotNull(processorFormatter);
        List<String> processorClasses =
                ((TargetsBuilders.JavaPluginFormatter) processorFormatter).getProcessorClasses();
        Assert.assertEquals(1, processorClasses.size());
        Assert.assertEquals(
                "com.spotify.dataenum.processor.DataEnumProcessor", processorClasses.get(0));
    }

    @Test
    public void testJarInspector_java_plugin_with_comments() throws Exception {
        final Dependency dependency =
                Dependency.builder().mavenCoordinate(mMavenCoordinate).build();
        final Function<Dependency, URI> dependencyURIFunction =
                dep -> {
                    try {
                        return Resources.getResource("dataenum-processor-1.0.2-with-comments.jar")
                                .toURI();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                };
        final TargetsBuilder processorFormatter =
                new RuleClassifiers.JarInspector(dependencyURIFunction)
                        .findAllPossibleBuilders(dependency)
                        .stream()
                        .filter(targetsBuilder -> targetsBuilder instanceof TargetsBuilders.JavaPluginFormatter)
                        .findFirst()
                        .orElse(null);
        Assert.assertNotNull(processorFormatter);
        List<String> processorClasses =
                ((TargetsBuilders.JavaPluginFormatter) processorFormatter).getProcessorClasses();
        Assert.assertEquals(1, processorClasses.size());
        Assert.assertEquals(
                "com.spotify.dataenum.processor.DataEnumProcessor", processorClasses.get(0));
    }

    @Test
    public void testJarInspector_java_plugin_native() throws Exception {
        final Dependency dependency =
                Dependency.builder().mavenCoordinate(mMavenCoordinate).build();
        final Function<Dependency, URI> dependencyURIFunction =
                dep -> {
                    try {
                        return RuleClassifiersTest.class
                                .getClassLoader()
                                .getResource("dataenum-processor-1.0.2.jar")
                                .toURI();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                };
        final TargetsBuilder processorFormatter =
                new RuleClassifiers.JarInspector(dependencyURIFunction)
                        .findAllPossibleBuilders(dependency)
                        .stream()
                        .filter(targetsBuilder -> targetsBuilder instanceof TargetsBuilders.JavaPluginFormatter)
                        .findFirst()
                        .orElse(null);
        Assert.assertNotNull(processorFormatter);
        List<String> processorClasses =
                ((TargetsBuilders.JavaPluginFormatter) processorFormatter).getProcessorClasses();
        Assert.assertEquals(1, processorClasses.size());
        Assert.assertEquals(
                "com.spotify.dataenum.processor.DataEnumProcessor", processorClasses.get(0));
    }

    @Test
    public void testPriority() {
        RuleClassifier classifier1 = Mockito.mock(RuleClassifier.class);
        Mockito.when(classifier1.classifyRule(Mockito.any())).thenReturn(Collections.emptyList());
        RuleClassifier classifier2 = Mockito.mock(RuleClassifier.class);
        TargetsBuilder targetsBuilder2 = Mockito.mock(TargetsBuilder.class);
        Mockito.when(classifier2.classifyRule(Mockito.any()))
                .thenReturn(Collections.singletonList(targetsBuilder2));
        RuleClassifier classifier3 = Mockito.mock(RuleClassifier.class);
        TargetsBuilder targetsBuilder3 = Mockito.mock(TargetsBuilder.class);
        Mockito.when(classifier3.classifyRule(Mockito.any()))
                .thenReturn(Collections.singletonList(targetsBuilder3));

        TargetsBuilder defaultTargetBuilder = Mockito.mock(TargetsBuilder.class);
        TargetsBuilder actualTargetBuilder =
                RuleClassifiers.priorityRuleClassifier(
                        Arrays.asList(classifier1, classifier2, classifier3),
                        defaultTargetBuilder,
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("g", "a", "1", ""))
                                .build());

        Assert.assertTrue(actualTargetBuilder instanceof TargetsBuilders.CompositeBuilder);
        TargetsBuilders.CompositeBuilder compositeBuilder = (TargetsBuilders.CompositeBuilder) actualTargetBuilder;
        Assert.assertEquals(1, compositeBuilder.getTargetsBuilders().size());
        Assert.assertTrue(compositeBuilder.getTargetsBuilders().contains(targetsBuilder2));
    }

    @Test
    public void testPriorityToDefault() {
        RuleClassifier classifier1 = Mockito.mock(RuleClassifier.class);
        Mockito.when(classifier1.classifyRule(Mockito.any())).thenReturn(Collections.emptyList());
        RuleClassifier classifier2 = Mockito.mock(RuleClassifier.class);
        Mockito.when(classifier2.classifyRule(Mockito.any())).thenReturn(Collections.emptyList());
        RuleClassifier classifier3 = Mockito.mock(RuleClassifier.class);
        Mockito.when(classifier3.classifyRule(Mockito.any())).thenReturn(Collections.emptyList());

        TargetsBuilder defaultTargetBuilder = Mockito.mock(TargetsBuilder.class);
        TargetsBuilder actualTargetBuilder =
                RuleClassifiers.priorityRuleClassifier(
                        Arrays.asList(classifier1, classifier2, classifier3),
                        defaultTargetBuilder,
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("g", "a", "1", ""))
                                .build());

        Assert.assertSame(defaultTargetBuilder, actualTargetBuilder);
    }

    @Test
    public void testJarClassifierNoResults() {
        RuleClassifier classifier = new RuleClassifiers.JarClassifier(dependency -> Collections.emptyList());

        final Dependency dependency =
                Dependency.builder().mavenCoordinate(mMavenCoordinate).build();

        Assert.assertTrue(classifier.classifyRule(dependency).isEmpty());
    }

    @Test
    public void testJarClassifierUnknownResults() {
        RuleClassifier classifier = new RuleClassifiers.JarClassifier(dependency -> Collections.singletonList(TargetsBuilders.AAR_IMPORT));

        final Dependency dependency =
                Dependency.builder().mavenCoordinate(mMavenCoordinate).build();

        //just passes that along
        Assert.assertEquals(1, classifier.classifyRule(dependency).size());
        Assert.assertSame(TargetsBuilders.AAR_IMPORT, classifier.classifyRule(dependency).get(0));
    }
}
