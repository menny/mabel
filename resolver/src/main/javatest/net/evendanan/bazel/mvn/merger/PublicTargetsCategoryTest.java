package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PublicTargetsCategoryTest {

    private Set<MavenCoordinate> mRootDependenciesAsSet;
    private List<Dependency> mResolvedDependencies;
    private List<Target> mAllTargets;

    @Before
    public void setup() {
        List<MavenCoordinate> rootDependencies = Arrays.asList(
                MavenCoordinate.create("a.b.c", "d", "1.1.1", "jar"),
                MavenCoordinate.create("b.c.d", "e", "2.1.1", "jar"),
                MavenCoordinate.create("c.d.e", "f", "3.1.1", "jar"));
        mRootDependenciesAsSet = new HashSet<>(rootDependencies);

        mResolvedDependencies =
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(rootDependencies.get(0))
                                .dependencies(
                                        Arrays.asList(
                                                MavenCoordinate.create(
                                                        "a.b.c.1", "d", "1.1.1", "jar"),
                                                MavenCoordinate.create(
                                                        "a.b.c.2", "d", "1.1.1", "jar")))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(rootDependencies.get(1))
                                .dependencies(
                                        Arrays.asList(
                                                MavenCoordinate.create(
                                                        "b.c.d.1", "e", "2.1.1", "jar"),
                                                rootDependencies.get(1)))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(rootDependencies.get(2))
                                .dependencies(
                                        Arrays.asList(
                                                MavenCoordinate.create(
                                                        "c.d.e.1", "f", "3.1.1", "jar"),
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

        mAllTargets =
                mResolvedDependencies.stream()
                        .map(
                                d ->
                                        Arrays.asList(
                                                createMainTarget(d),
                                                createAliasTarget(d),
                                                createSecondaryTarget(d)))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
    }

    private Target createAliasTarget(Dependency dependency) {
        return new Target(
                dependency.mavenCoordinate().toMavenString(),
                "alias",
                String.format(
                        Locale.ROOT,
                        "%s_%s",
                        dependency.mavenCoordinate().artifactId(),
                        dependency.mavenCoordinate().groupId()))
                .setPublicVisibility();
    }

    private Target createSecondaryTarget(Dependency dependency) {
        return new Target(
                dependency.mavenCoordinate().toMavenString(),
                "second_rule",
                String.format(
                        Locale.ROOT,
                        "%s_%s_second",
                        dependency.mavenCoordinate().artifactId(),
                        dependency.mavenCoordinate().groupId()))
                .setPrivateVisibility();
    }

    private Target createMainTarget(Dependency dependency) {
        return new Target(
                dependency.mavenCoordinate().toMavenString(),
                "main_rule",
                dependency.mavenCoordinate().toMavenString().replace(':', '_'))
                .setPublicVisibility();
    }

    @Test
    public void teatAllCategoriesExist() {
        for (PublicTargetsCategory.Type value : PublicTargetsCategory.Type.values()) {
            Assert.assertNotNull(
                    PublicTargetsCategory.create(value, mRootDependenciesAsSet, mResolvedDependencies));
        }
    }

    @Test
    public void testCategoryAll() {
        Function<Target, Target> underTest =
                PublicTargetsCategory.create(
                        PublicTargetsCategory.Type.all, mRootDependenciesAsSet, mResolvedDependencies);

        // stays the same
        for (Target target : mAllTargets) {
            final boolean isPublic = target.isPublic();
            Target fixedTarget = underTest.apply(target);
            Assert.assertSame(fixedTarget, target);
            Assert.assertEquals(isPublic, fixedTarget.isPublic());
        }
    }

    @Test
    public void testCategoryRequestedDeps() {
        Function<Target, Target> underTest =
                PublicTargetsCategory.create(
                        PublicTargetsCategory.Type.requested_deps,
                        mRootDependenciesAsSet,
                        mResolvedDependencies);

        final Set<String> rootMavens =
                mRootDependenciesAsSet.stream()
                        .map(MavenCoordinate::toMavenString)
                        .collect(Collectors.toSet());
        // stays the same
        for (Target target : mAllTargets) {
            final boolean isPublic = target.isPublic();
            final boolean isRootDependency = rootMavens.contains(target.getMavenCoordinates());
            Target fixedTarget = underTest.apply(target);
            Assert.assertSame(fixedTarget, target);
            if (!isPublic) {
                Assert.assertFalse(fixedTarget.isPublic());
            } else {
                Assert.assertEquals(isRootDependency, fixedTarget.isPublic());
            }
        }
    }
}
