package net.evendanan.bazel.mvn.merger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;
import org.junit.Assert;
import org.junit.Test;

public class TestOnlyMarkerTest {

  @Test
  public void testMarkingHappyPath() {
    final Dependency junitDep =
        Dependency.builder()
            .mavenCoordinate(MavenCoordinate.create("junit", "junit", "2", "jar"))
            .build();
    final Resolution junit =
        Resolution.create(junitDep.mavenCoordinate(), Collections.singleton(junitDep));

    final Resolution appTestUtil =
        Resolution.create(
            MavenCoordinate.create("util", "junit-helper", "1", "jar"),
            Arrays.asList(
                junitDep,
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("a", "b", "1", "jar"))
                    .dependencies(Collections.singleton(junitDep.mavenCoordinate()))
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("util", "junit-helper", "1", "jar"))
                    .dependencies(
                        Collections.singleton(MavenCoordinate.create("a", "b", "1", "jar")))
                    .build()));

    final HashSet<MavenCoordinate> initialTestOnlyMap =
        new HashSet<>(Arrays.asList(junitDep.mavenCoordinate(), appTestUtil.rootDependency()));
    final Predicate<MavenCoordinate> marked =
        TestOnlyMarker.mark(Arrays.asList(junit, appTestUtil), initialTestOnlyMap);

    Assert.assertTrue(marked.test(junit.rootDependency()));
    Assert.assertTrue(marked.test(appTestUtil.rootDependency()));
    Assert.assertTrue(marked.test(MavenCoordinate.create("a", "b", "1", "jar")));
    // testing strip-down
    Assert.assertTrue(marked.test(MavenCoordinate.create("a", "b", "2", "jar")));
    Assert.assertTrue(marked.test(MavenCoordinate.create("a", "b", "1", "aar")));
    // random
    Assert.assertFalse(marked.test(MavenCoordinate.create("b", "b", "1", "jar")));
  }

  @Test
  public void testMarkingHappyPathWithNonTestOnly() {
    final Dependency junitDep =
        Dependency.builder()
            .mavenCoordinate(MavenCoordinate.create("junit", "junit", "2", "jar"))
            .build();
    final Resolution junit =
        Resolution.create(junitDep.mavenCoordinate(), Collections.singleton(junitDep));

    final Resolution appUtil =
        Resolution.create(
            MavenCoordinate.create("util", "helper", "1", "jar"),
            Arrays.asList(
                junitDep,
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("a", "b", "1", "jar"))
                    .dependencies(Collections.emptyList())
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("util", "helper", "1", "jar"))
                    .dependencies(
                        Collections.singleton(MavenCoordinate.create("a", "b", "1", "jar")))
                    .build()));

    final Set<MavenCoordinate> initialTestOnlyMap =
        Collections.singleton(junitDep.mavenCoordinate());
    final Predicate<MavenCoordinate> marked =
        TestOnlyMarker.mark(Arrays.asList(junit, appUtil), initialTestOnlyMap);

    Assert.assertTrue(marked.test(junit.rootDependency()));
    Assert.assertFalse(marked.test(appUtil.rootDependency()));
    Assert.assertFalse(marked.test(MavenCoordinate.create("a", "b", "1", "jar")));
  }

  @Test
  public void testMarkingWhenTestOnlyDepHasDifferentVersion() {
    final Dependency junitDep =
        Dependency.builder()
            .mavenCoordinate(MavenCoordinate.create("junit", "junit", "2", "jar"))
            .build();
    final Resolution junit =
        Resolution.create(junitDep.mavenCoordinate(), Collections.singleton(junitDep));

    final Dependency junitOld =
        Dependency.builder(junitDep)
            .mavenCoordinate(MavenCoordinate.create("junit", "junit", "1", "jar"))
            .build();
    final Resolution appTestUtil =
        Resolution.create(
            MavenCoordinate.create("util", "junit-helper", "1", "jar"),
            Arrays.asList(
                junitOld,
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("a", "b", "1", "jar"))
                    .dependencies(Collections.singleton(junitOld.mavenCoordinate()))
                    .build(),
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("util", "junit-helper", "1", "jar"))
                    .dependencies(
                        Collections.singleton(MavenCoordinate.create("a", "b", "1", "jar")))
                    .build()));

    final Predicate<MavenCoordinate> marked =
        TestOnlyMarker.mark(
            Arrays.asList(junit, appTestUtil),
            new HashSet<>(Arrays.asList(junitDep.mavenCoordinate(), appTestUtil.rootDependency())));

    Assert.assertTrue(marked.test(junit.rootDependency()));
    Assert.assertTrue(marked.test(MavenCoordinate.create("junit", "junit", "1", "jar")));
    Assert.assertTrue(marked.test(appTestUtil.rootDependency()));
    Assert.assertTrue(marked.test(MavenCoordinate.create("a", "b", "1", "jar")));
    Assert.assertTrue(marked.test(MavenCoordinate.create("a", "b", "2", "jar")));
  }

  @Test(expected = GraphVerifications.InvalidGraphException.class)
  public void testThrowsExceptionIfRootIsMarkedAsTestOnlyByMarker() {
    final Dependency junitDep =
        Dependency.builder()
            .mavenCoordinate(MavenCoordinate.create("junit", "junit", "1", "jar"))
            .build();
    final Resolution junit =
        Resolution.create(junitDep.mavenCoordinate(), Collections.singleton(junitDep));

    final Resolution appTestUtil =
        Resolution.create(
            MavenCoordinate.create("util", "junit-helper", "1", "jar"),
            Arrays.asList(
                junitDep,
                Dependency.builder()
                    .mavenCoordinate(MavenCoordinate.create("util", "junit-helper", "1", "jar"))
                    .dependencies(Collections.singleton(junitDep.mavenCoordinate()))
                    .build()));

    TestOnlyMarker.mark(
        Arrays.asList(junit, appTestUtil), Collections.singleton(junitDep.mavenCoordinate()));
  }

  @Test
  public void testReturnsSameIfInitialMapIsEmpty() {
    final Set<MavenCoordinate> emptySet = new HashSet<>();
    final Predicate<MavenCoordinate> marked =
        TestOnlyMarker.mark(
            Collections.singleton(
                Resolution.create(
                    MavenCoordinate.create("a", "b", "1", "jar"), Collections.emptyList())),
            emptySet);

    Assert.assertFalse(marked.test(MavenCoordinate.create("a", "b", "1", "jar")));
  }
}
