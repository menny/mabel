package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TargetCommenterTest {
    private TargetCommenter mUnderTest;
    private List<MavenCoordinate> mRootDependencies;
    private Set<MavenCoordinate> mRootDependenciesAsSet;
    private List<Dependency> mResolvedDependencies;
    private TargetsBuilder mMockBaseBuilder;
    private DependencyTools mDependencyTools;
    private List<Target> mReturned;

    @Before
    public void setup() {
        mReturned = new ArrayList<>();
        mDependencyTools = new DependencyTools();
        mRootDependencies = Arrays.asList(
                MavenCoordinate.create("a.b.c", "d", "1.1.1", "jar"),
                MavenCoordinate.create("b.c.d", "e", "2.1.1", "jar"),
                MavenCoordinate.create("c.d.e", "f", "3.1.1", "jar"));
        mRootDependenciesAsSet = new HashSet<>(mRootDependencies);

        mResolvedDependencies =
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(mRootDependencies.get(0))
                                .dependencies(
                                        Arrays.asList(
                                                MavenCoordinate.create(
                                                        "a.b.c.1", "d", "1.1.1", "jar"),
                                                MavenCoordinate.create(
                                                        "a.b.c.2", "d", "1.1.1", "jar")))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(mRootDependencies.get(1))
                                .dependencies(
                                        Arrays.asList(
                                                MavenCoordinate.create(
                                                        "b.c.d.1", "e", "2.1.1", "jar"),
                                                mRootDependencies.get(0),
                                                mRootDependencies.get(2)))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(mRootDependencies.get(2))
                                .dependencies(
                                        Arrays.asList(
                                                MavenCoordinate.create(
                                                        "c.d.e.1", "f", "3.1.1", "jar"),
                                                MavenCoordinate.create(
                                                        "a.b.c.2", "d", "1.1.1", "jar"),
                                                MavenCoordinate.create(
                                                        "c.d.e.2", "f", "3.1.1", "jar")))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create("a.b.c.1", "d", "1.1.1", "jar"))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create("a.b.c.2", "d", "1.1.1", "jar"))
                                .dependencies(
                                        Collections.singleton(
                                                MavenCoordinate.create(
                                                        "a.b.c.2.1", "d", "1.1.1", "jar")))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create("b.c.d.1", "e", "2.1.1", "jar"))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create("c.d.e.1", "f", "3.1.1", "jar"))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create("c.d.e.2", "f", "3.1.1", "jar"))
                                .dependencies(
                                        Arrays.asList(
                                                MavenCoordinate.create(
                                                        "c.d.e.2.1", "f", "3.1.1", "jar"),
                                                MavenCoordinate.create(
                                                        "c.d.e.2.2", "f", "3.1.1", "jar"),
                                                MavenCoordinate.create(
                                                        "c.d.e.2.2", "f", "3.1.1", "jar")))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create("c.d.e.2.1", "f", "3.1.1", "jar"))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create("c.d.e.2.2", "f", "3.1.1", "jar"))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create("a.b.c.2.1", "f", "3.1.1", "jar"))
                                .build());

        mMockBaseBuilder = Mockito.mock(TargetsBuilder.class);
        Mockito.doReturn(mReturned).when(mMockBaseBuilder).buildTargets(Mockito.any(), Mockito.any());
        mUnderTest = new TargetCommenter(mRootDependenciesAsSet, mResolvedDependencies);
    }

    @Test
    public void testDelegateToBase() {
        mReturned.add(createTarget(mRootDependencies.get(0)));
        mReturned.add(createTarget(mRootDependencies.get(1)));
        final List<Target> targets = mUnderTest.createTargetBuilder(mMockBaseBuilder)
                .buildTargets(mResolvedDependencies.get(0), mDependencyTools);
        Mockito.verify(mMockBaseBuilder).buildTargets(Mockito.same(mResolvedDependencies.get(0)), Mockito.same(mDependencyTools));
        Assert.assertSame(mReturned, targets);
        Assert.assertFalse(mReturned.isEmpty());
        for (int targetIndex = 0; targetIndex < mReturned.size(); targetIndex++) {
            Assert.assertSame(targets.get(targetIndex), mReturned.get(targetIndex));
        }
    }

    @Test
    public void testAddsCommentIfRootDependency() {
        final Target target = createTarget(mRootDependencies.get(1));
        Assert.assertTrue(target.getComments().isEmpty());
        mReturned.add(target);
        final List<Target> targets = mUnderTest.createTargetBuilder(mMockBaseBuilder)
                .buildTargets(mResolvedDependencies.get(0), mDependencyTools);
        Assert.assertEquals(1, target.getComments().size());
        List<String> comments = new ArrayList<>(target.getComments());
        Assert.assertEquals("This is a root requested Maven artifact.", comments.get(0));
    }

    @Test
    public void testAddsCommentIfDependencyIsRequiredByRootDependency() {
        final Target target = createTarget(MavenCoordinate.create("a.b.c.1", "d", "1.1.1", "jar"));
        Assert.assertTrue(target.getComments().isEmpty());
        mReturned.add(target);
        final List<Target> targets = mUnderTest.createTargetBuilder(mMockBaseBuilder)
                .buildTargets(mResolvedDependencies.get(0), mDependencyTools);
        Assert.assertEquals(1, target.getComments().size());
        List<String> comments = new ArrayList<>(target.getComments());
        Assert.assertEquals("This is a dependency of '"+mRootDependencies.get(0).toMavenString() + "'.", comments.get(0));
    }

    @Test
    public void testAddsAllRequiringDependencies() {
        final Target target = createTarget(MavenCoordinate.create("a.b.c.2", "d", "1.1.1", "jar"));
        Assert.assertTrue(target.getComments().isEmpty());
        mReturned.add(target);
        final List<Target> targets = mUnderTest.createTargetBuilder(mMockBaseBuilder)
                .buildTargets(mResolvedDependencies.get(0), mDependencyTools);
        Assert.assertEquals(2, target.getComments().size());
        List<String> comments = new ArrayList<>(target.getComments());
        //note, this is sorted, too
        Assert.assertEquals("This is a dependency of 'a.b.c:d:1.1.1'.", comments.get(0));
        Assert.assertEquals("This is a dependency of 'c.d.e:f:3.1.1'.", comments.get(1));
    }

    @Test
    public void testAddsCommentIfDependencyIsRequiredByNoneRootDependency() {
        final Target target = createTarget(MavenCoordinate.create("c.d.e.2.2", "f", "3.1.1", "jar"));
        Assert.assertTrue(target.getComments().isEmpty());
        mReturned.add(target);
        final List<Target> targets = mUnderTest.createTargetBuilder(mMockBaseBuilder)
                .buildTargets(mResolvedDependencies.get(0), mDependencyTools);
        Assert.assertEquals(1, target.getComments().size());
        List<String> comments = new ArrayList<>(target.getComments());
        Assert.assertEquals("This is a dependency of 'c.d.e.2:f:3.1.1'.", comments.get(0));
    }

    @Test
    public void testAddsCommentsIfRootDependencyButAlsoRequiredDependency() {
        final Target target = createTarget(mRootDependencies.get(2));
        Assert.assertTrue(target.getComments().isEmpty());
        mReturned.add(target);
        final List<Target> targets = mUnderTest.createTargetBuilder(mMockBaseBuilder)
                .buildTargets(mResolvedDependencies.get(0), mDependencyTools);
        Assert.assertEquals(2, target.getComments().size());
        List<String> comments = new ArrayList<>(target.getComments());
        Assert.assertEquals("This is a root requested Maven artifact.", comments.get(0));
        Assert.assertEquals("This is a dependency of 'b.c.d:e:2.1.1'.", comments.get(1));
    }

    private static Target createTarget(MavenCoordinate mavenCoordinate) {
        return new Target(mavenCoordinate.toMavenString(), "rule", mavenCoordinate.toMavenString().replace(":", "_"));
    }
}