package net.evendanan.bazel.mvn.impl;

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
import java.util.List;
import java.util.Optional;
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
                TargetsBuilders.AAR_IMPORT,
                new RuleClassifiers.AarClassifier().classifyRule(dep).orElse(null));

        dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "jar"))
                        .build();
        Assert.assertSame(Optional.empty(), new RuleClassifiers.AarClassifier().classifyRule(dep));

        dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "pom"))
                        .build();
        Assert.assertSame(Optional.empty(), new RuleClassifiers.AarClassifier().classifyRule(dep));
    }

    @Test
    public void testPomClassifier() {
        Dependency dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "pom"))
                        .build();
        Assert.assertSame(
                TargetsBuilders.JAVA_IMPORT,
                new RuleClassifiers.PomClassifier().classifyRule(dep).orElse(null));

        dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "jar"))
                        .build();
        Assert.assertSame(Optional.empty(), new RuleClassifiers.PomClassifier().classifyRule(dep));
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
        Assert.assertSame(
                Optional.empty(),
                new RuleClassifiers.JarInspector(dependencyURIFunction).classifyRule(dependency));
    }

    @Test
    public void testJarInspector_java_plugin() throws Exception {
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
                        .classifyRule(dependency)
                        .orElse(null);
        Assert.assertNotNull(processorFormatter);
        Assert.assertTrue(processorFormatter instanceof TargetsBuilders.JavaPluginFormatter);
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
                        return RuleClassifiersTest.class
                                .getClassLoader()
                                .getResource("dataenum-processor-1.0.2-with-comments.jar")
                                .toURI();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                };
        final TargetsBuilder processorFormatter =
                new RuleClassifiers.JarInspector(dependencyURIFunction)
                        .classifyRule(dependency)
                        .orElse(null);
        Assert.assertNotNull(processorFormatter);
        Assert.assertTrue(processorFormatter instanceof TargetsBuilders.JavaPluginFormatter);
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
                        .classifyRule(dependency)
                        .orElse(null);
        Assert.assertNotNull(processorFormatter);
        Assert.assertTrue(processorFormatter instanceof TargetsBuilders.JavaPluginFormatter);
        List<String> processorClasses =
                ((TargetsBuilders.JavaPluginFormatter) processorFormatter).getProcessorClasses();
        Assert.assertEquals(1, processorClasses.size());
        Assert.assertEquals(
                "com.spotify.dataenum.processor.DataEnumProcessor", processorClasses.get(0));
    }

    @Test
    public void testJarInspector_kotlin() throws Exception {
        final Dependency dependency =
                Dependency.builder().mavenCoordinate(mMavenCoordinate).build();
        final Function<Dependency, URI> dependencyURIFunction =
                dep -> {
                    try {
                        return RuleClassifiersTest.class
                                .getClassLoader()
                                .getResource("mockk-1.0.jar")
                                .toURI();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                };

        Assert.assertSame(
                TargetsBuilders.KOTLIN_IMPORT,
                new RuleClassifiers.JarInspector(dependencyURIFunction)
                        .classifyRule(dependency)
                        .orElse(null));
    }

//    @Test
//    public void testJarInspector_kotlin_android() throws Exception {
//        final Dependency dependency =
//                Dependency.builder().mavenCoordinate(mMavenCoordinate).build();
//        final Function<Dependency, URI> dependencyURIFunction =
//                dep -> {
//                    try {
//                        return RuleClassifiersTest.class
//                                .getClassLoader()
//                                .getResource("mockk-1.0-for-android.aar")
//                                .toURI();
//                    } catch (URISyntaxException e) {
//                        throw new RuntimeException(e);
//                    }
//                };
//
//        Assert.assertSame(
//                TargetsBuilders.KOTLIN_ANDROID_IMPORT,
//                new RuleClassifiers.JarInspector(dependencyURIFunction)
//                        .classifyRule(dependency)
//                        .orElse(null));
//    }

    @Test
    public void testPriority() {
        RuleClassifier classifier1 = Mockito.mock(RuleClassifier.class);
        Mockito.when(classifier1.classifyRule(Mockito.any())).thenReturn(Optional.empty());
        RuleClassifier classifier2 = Mockito.mock(RuleClassifier.class);
        TargetsBuilder targetsBuilder2 = Mockito.mock(TargetsBuilder.class);
        Mockito.when(classifier2.classifyRule(Mockito.any()))
                .thenReturn(Optional.of(targetsBuilder2));
        RuleClassifier classifier3 = Mockito.mock(RuleClassifier.class);
        TargetsBuilder targetsBuilder3 = Mockito.mock(TargetsBuilder.class);
        Mockito.when(classifier3.classifyRule(Mockito.any()))
                .thenReturn(Optional.of(targetsBuilder3));

        TargetsBuilder defaultTargetBuilder = Mockito.mock(TargetsBuilder.class);
        TargetsBuilder actualTargetBuilder =
                RuleClassifiers.priorityRuleClassifier(
                        Arrays.asList(classifier1, classifier2, classifier3),
                        defaultTargetBuilder,
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("g", "a", "1", ""))
                                .build());

        Assert.assertSame(targetsBuilder2, actualTargetBuilder);
    }

    @Test
    public void testPriorityToDefault() {
        RuleClassifier classifier1 = Mockito.mock(RuleClassifier.class);
        Mockito.when(classifier1.classifyRule(Mockito.any())).thenReturn(Optional.empty());
        RuleClassifier classifier2 = Mockito.mock(RuleClassifier.class);
        Mockito.when(classifier2.classifyRule(Mockito.any())).thenReturn(Optional.empty());
        RuleClassifier classifier3 = Mockito.mock(RuleClassifier.class);
        Mockito.when(classifier3.classifyRule(Mockito.any())).thenReturn(Optional.empty());

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
}
