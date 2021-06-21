package net.evendanan.bazel.mvn.impl;

import com.google.common.io.Resources;

import net.evendanan.bazel.mvn.api.RuleClassifier;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
    public void testNaiveKotlin() {
        Dependency dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "jar"))
                        .dependencies(
                                Collections.singletonList(
                                        MavenCoordinate.create("org.jetbrains.kotlin", "kotlin-stdlib", "1.4.31", "jar")
                                )
                        )
                        .build();
        Assert.assertSame(
                TargetsBuilders.KOTLIN_IMPORT,
                new RuleClassifiers.NaiveKotlinClassifier().classifyRule(dep).get(0));

        dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "jar"))
                        .build();
        Assert.assertTrue(new RuleClassifiers.NaiveKotlinClassifier().classifyRule(dep).isEmpty());
    }

    @Test
    public void testNaiveAndroidKotlin() {
        Dependency dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "aar"))
                        .dependencies(
                                Collections.singletonList(
                                        MavenCoordinate.create("org.jetbrains.kotlin", "kotlin-stdlib", "1.4.31", "jar")
                                )
                        )
                        .build();
        Assert.assertSame(
                TargetsBuilders.KOTLIN_ANDROID_IMPORT,
                new RuleClassifiers.NaiveKotlinAarClassifier().classifyRule(dep).get(0));

        dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "jar"))
                        .dependencies(
                                Collections.singletonList(
                                        MavenCoordinate.create("org.jetbrains.kotlin", "kotlin-stdlib", "1.4.31", "jar")
                                )
                        )
                        .build();
        Assert.assertTrue(new RuleClassifiers.NaiveKotlinAarClassifier().classifyRule(dep).isEmpty());

        dep =
                Dependency.builder()
                        .mavenCoordinate(MavenCoordinate.create("", "", "", "aar"))
                        .build();
        Assert.assertTrue(new RuleClassifiers.NaiveKotlinAarClassifier().classifyRule(dep).isEmpty());
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
                        .findAllPossibleBuilders(dependency)
                        .get(0));
    }

    @Test
    public void testJarInspector_kotlin_android() throws Exception {
        final Dependency dependency =
                Dependency.builder().mavenCoordinate(mMavenCoordinate).build();
        final Function<Dependency, URI> dependencyURIFunction =
                dep -> {
                    try {
                        return Resources.getResource("mockk-1.0-for-android.aar")
                                .toURI();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                };

        Assert.assertTrue(
                new RuleClassifiers.JarInspector(dependencyURIFunction)
                        .findAllPossibleBuilders(dependency)
                        //This should actually be KOTLIN_IMPORT_ANDROID.
                        //The inspector only
                        .contains(TargetsBuilders.KOTLIN_IMPORT));
    }

    @Test
    public void testJarInspector_kotlin_android_jar() throws Exception {
        final Dependency dependency =
                Dependency.builder().mavenCoordinate(mMavenCoordinate).build();
        final Function<Dependency, URI> dependencyURIFunction =
                dep -> {
                    try {
                        return Resources.getResource("mockk-1.0-for-android.jar")
                                .toURI();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                };

        Assert.assertTrue(
                new RuleClassifiers.JarInspector(dependencyURIFunction)
                        .findAllPossibleBuilders(dependency)
                        //This should actually be KOTLIN_IMPORT_ANDROID.
                        //The inspector only
                        .contains(TargetsBuilders.KOTLIN_IMPORT));
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

    @Test
    public void testJarClassifierKotlinJar() {
        RuleClassifier classifier = new RuleClassifiers.JarClassifier(dependency -> Collections.singletonList(TargetsBuilders.KOTLIN_IMPORT));

        final Dependency dependency =
                Dependency.builder()
                        .mavenCoordinate(mMavenCoordinate)
                        .url("https://example.com/artifact.jar")
                        .build();

        List<TargetsBuilder> targetsBuilder = classifier.classifyRule(dependency);
        Assert.assertEquals(1, targetsBuilder.size());
        Assert.assertSame(TargetsBuilders.KOTLIN_IMPORT, targetsBuilder.get(0));
    }

    @Test
    public void testJarClassifierMultipleKotlin() {
        RuleClassifier classifier = new RuleClassifiers.JarClassifier(dependency -> Arrays.asList(
                TargetsBuilders.KOTLIN_IMPORT,
                TargetsBuilders.KOTLIN_IMPORT,
                TargetsBuilders.KOTLIN_IMPORT,
                TargetsBuilders.KOTLIN_IMPORT));

        final Dependency dependency =
                Dependency.builder()
                        .mavenCoordinate(mMavenCoordinate)
                        .url("https://example.com/artifact.jar")
                        .build();

        List<TargetsBuilder> targetsBuilder = classifier.classifyRule(dependency);
        Assert.assertEquals(4, targetsBuilder.size());
        Assert.assertSame(TargetsBuilders.KOTLIN_IMPORT, targetsBuilder.get(0));
        Assert.assertSame(TargetsBuilders.KOTLIN_IMPORT, targetsBuilder.get(1));
        Assert.assertSame(TargetsBuilders.KOTLIN_IMPORT, targetsBuilder.get(2));
        Assert.assertSame(TargetsBuilders.KOTLIN_IMPORT, targetsBuilder.get(3));
    }

    @Test
    public void testJarClassifierKotlinJarAndUnknown() {
        RuleClassifier classifier = new RuleClassifiers.JarClassifier(dependency -> Arrays.asList(
                TargetsBuilders.KOTLIN_IMPORT,
                TargetsBuilders.JAVA_IMPORT));

        final Dependency dependency =
                Dependency.builder()
                        .mavenCoordinate(mMavenCoordinate)
                        .url("https://example.com/artifact.jar")
                        .build();

        List<TargetsBuilder> targetsBuilder = classifier.classifyRule(dependency);
        Assert.assertEquals(2, targetsBuilder.size());
        Assert.assertSame(TargetsBuilders.KOTLIN_IMPORT, targetsBuilder.get(0));
        Assert.assertSame(TargetsBuilders.JAVA_IMPORT, targetsBuilder.get(1));
    }

    @Test
    @Ignore("until we figure out kotlin-android import")
    public void testJarClassifierKotlinAar() {
        RuleClassifier classifier = new RuleClassifiers.JarClassifier(dependency -> Collections.singletonList(TargetsBuilders.KOTLIN_IMPORT));

        final Dependency dependency =
                Dependency.builder()
                        .mavenCoordinate(mMavenCoordinate)
                        .url("https://example.com/artifact.aar")
                        .build();

        List<TargetsBuilder> targetsBuilder = classifier.classifyRule(dependency);
        Assert.assertEquals(1, targetsBuilder.size());
        Assert.assertSame(TargetsBuilders.KOTLIN_ANDROID_IMPORT, targetsBuilder.get(0));
    }

    @Test
    @Ignore("until we figure out kotlin-android import")
    public void testJarClassifierKotlinAarAndPlugin() {
        //TODO: at some point, this should return a kotlin compiler plugin, or something....
        RuleClassifier classifier = new RuleClassifiers.JarClassifier(dependency -> Arrays.asList(
                TargetsBuilders.KOTLIN_IMPORT,
                new TargetsBuilders.JavaPluginFormatter(Collections.singletonList("com.example.Processor"))));

        final Dependency dependency =
                Dependency.builder()
                        .mavenCoordinate(mMavenCoordinate)
                        .url("https://example.com/artifact.aar")
                        .build();

        List<TargetsBuilder> targetsBuilder = classifier.classifyRule(dependency);
        Assert.assertFalse(targetsBuilder.isEmpty());
        Assert.assertSame(TargetsBuilders.KOTLIN_ANDROID_IMPORT, targetsBuilder.get(0));
    }

    @Test
    public void testJarClassifierKotlinJarAndPlugin() {
        RuleClassifier classifier = new RuleClassifiers.JarClassifier(dependency -> Arrays.asList(
                TargetsBuilders.KOTLIN_IMPORT,
                new TargetsBuilders.JavaPluginFormatter(Collections.singletonList("com.example.Processor"))));

        final Dependency dependency =
                Dependency.builder()
                        .mavenCoordinate(mMavenCoordinate)
                        .url("https://example.com/artifact.jar")
                        .build();

        List<TargetsBuilder> targetsBuilder = classifier.classifyRule(dependency);
        Assert.assertEquals(2, targetsBuilder.size());
        Assert.assertSame(TargetsBuilders.KOTLIN_IMPORT, targetsBuilder.get(0));
        Assert.assertTrue(targetsBuilder.get(1) instanceof TargetsBuilders.JavaPluginFormatter);
        TargetsBuilders.JavaPluginFormatter plugin = (TargetsBuilders.JavaPluginFormatter) targetsBuilder.get(1);
        Assert.assertEquals(1, plugin.getProcessorClasses().size());
        Assert.assertEquals("com.example.Processor", plugin.getProcessorClasses().get(0));
    }

    @Test
    public void testJarClassifierKotlinJarAndPluginReverseOrder() {
        RuleClassifier classifier = new RuleClassifiers.JarClassifier(dependency -> Arrays.asList(
                new TargetsBuilders.JavaPluginFormatter(Collections.singletonList("com.example.Processor")),
                TargetsBuilders.KOTLIN_IMPORT));

        final Dependency dependency =
                Dependency.builder()
                        .mavenCoordinate(mMavenCoordinate)
                        .url("https://example.com/artifact.jar")
                        .build();

        List<TargetsBuilder> targetsBuilder = classifier.classifyRule(dependency);
        Assert.assertEquals(2, targetsBuilder.size());
        //kotlin first always!
        Assert.assertSame(TargetsBuilders.KOTLIN_IMPORT, targetsBuilder.get(0));
        Assert.assertTrue(targetsBuilder.get(1) instanceof TargetsBuilders.JavaPluginFormatter);
        TargetsBuilders.JavaPluginFormatter plugin = (TargetsBuilders.JavaPluginFormatter) targetsBuilder.get(1);
        Assert.assertEquals(1, plugin.getProcessorClasses().size());
        Assert.assertEquals("com.example.Processor", plugin.getProcessorClasses().get(0));
    }
}
