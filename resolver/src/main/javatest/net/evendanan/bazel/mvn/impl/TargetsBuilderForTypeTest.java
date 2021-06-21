package net.evendanan.bazel.mvn.impl;

import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.TargetType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class TargetsBuilderForTypeTest {

    private Dependency dependency;

    @Before
    public void setup() {
        dependency = Dependency.builder()
                .mavenCoordinate(MavenCoordinate.create("g", "a", "1", "jar"))
                .build();
    }

    @Test
    public void generateBuilderForTheRightType() {
        AtomicReference<TargetType> returnValue = new AtomicReference<>(TargetType.jar);
        TargetsBuilderForType underTest = new TargetsBuilderForType(mvn -> returnValue.get(), dep -> null);

        returnValue.set(TargetType.jar);
        Assert.assertSame(TargetsBuilders.JAVA_IMPORT, underTest.generateBuilder(dependency));

        returnValue.set(TargetType.aar);
        Assert.assertSame(TargetsBuilders.AAR_IMPORT_WITHOUT_EXPORTS, underTest.generateBuilder(dependency));

        returnValue.set(TargetType.naive);
        Assert.assertTrue(underTest.generateBuilder(dependency) instanceof TargetsBuilderForType.NaiveBuilder);

        returnValue.set(TargetType.processor);
        Assert.assertTrue(underTest.generateBuilder(dependency) instanceof TargetsBuilderForType.ProcessorBuilder);

        returnValue.set(TargetType.auto);
        Assert.assertTrue(underTest.generateBuilder(dependency) instanceof TargetsBuilderForType.AutoBuilder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsExceptionOnNullType() {
        TargetsBuilderForType underTest = new TargetsBuilderForType(mvn -> null, dep -> null);
        underTest.generateBuilder(dependency);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsExceptionOnUnhandledType() {
        TargetsBuilderForType underTest = new TargetsBuilderForType(mvn -> TargetType.valueOf("unknown"), dep -> null);
        underTest.generateBuilder(dependency);
    }
}