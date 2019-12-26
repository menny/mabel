package net.evendanan.bazel.mvn.impl;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class CompositeFormatterTest {

    @Test
    public void testCompositeFormatter() {
        final TargetsBuilder formatter1 = Mockito.mock(TargetsBuilder.class);
        Mockito.doReturn(Collections.singletonList(new Target("a:b:3", "rule1", "name1"))).when(formatter1).buildTargets(Mockito.any(), Mockito.any());
        final TargetsBuilder formatter2 = Mockito.mock(TargetsBuilder.class);
        Mockito.doReturn(Collections.singletonList(new Target("a:b:4", "rule2", "name2"))).when(formatter2).buildTargets(Mockito.any(), Mockito.any());

        final TargetsBuilders.CompositeBuilder compositeBuilder = new TargetsBuilders.CompositeBuilder(formatter1, formatter2);

        Dependency dependency = Dependency.newBuilder().build();

        final List<Target> targets = compositeBuilder.buildTargets(dependency, DependencyTools.DEFAULT);

        Mockito.verify(formatter1).buildTargets(Mockito.same(dependency), Mockito.same(DependencyTools.DEFAULT));
        Mockito.verifyNoMoreInteractions(formatter1);
        Mockito.verify(formatter2).buildTargets(Mockito.same(dependency), Mockito.same(DependencyTools.DEFAULT));
        Mockito.verifyNoMoreInteractions(formatter2);


        Assert.assertEquals("rule1", targets.get(0).getRuleName());
        Assert.assertEquals("rule2", targets.get(1).getRuleName());
    }
}