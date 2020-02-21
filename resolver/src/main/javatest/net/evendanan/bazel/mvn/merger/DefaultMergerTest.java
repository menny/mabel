package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class DefaultMergerTest {

    private Resolution mBasicResolution;
    private String mBasicResolutionGraph;

    @Before
    public void setup() {
        mBasicResolution = Resolution.create(
                MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""))
                                .dependencies(Collections.singleton(MavenCoordinate.create("net.evendanan", "inner1", "0.1", "")))
                                .url("http://example.com/artifact1.jar")
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.1", ""))
                                .url("http://example.com/artifact2.jar")
                                .dependencies(Arrays.asList(
                                        MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""),
                                        MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", "")
                                ))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""))
                                .url("http://example.com/artifact3.jar")
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", ""))
                                .url("http://example.com/artifact4.jar")
                                .build()));
        mBasicResolutionGraph = GraphUtils.printGraph(mBasicResolution);
    }

    @Test
    public void testExcludes_NoRemovalIfEmptyExclude() {
        Collection<Dependency> actual = ExcludesFilter.filterDependencies(mBasicResolution.allResolvedDependencies(),
                Collections.emptyList());

        Assert.assertEquals(mBasicResolution.allResolvedDependencies().size(), actual.size());
        actual.forEach(dep -> Assert.assertTrue(containsDependency(mBasicResolution.allResolvedDependencies(), dep)));

        Assert.assertEquals(mBasicResolutionGraph,
                GraphUtils.printGraph(Resolution.create(mBasicResolution.rootDependency(), actual)));
    }

    @Test
    public void testExcludes_NoRemovalIfNoMatches() {
        Collection<Dependency> actual = ExcludesFilter.filterDependencies(mBasicResolution.allResolvedDependencies(),
                Arrays.asList("some:other:1.2", "net.evendanan:dep1:333.00", "net.evendanan:dep5"));

        Assert.assertEquals(mBasicResolution.allResolvedDependencies().size(), actual.size());
        actual.forEach(dep -> Assert.assertTrue(containsDependency(mBasicResolution.allResolvedDependencies(), dep)));

        Assert.assertEquals(mBasicResolutionGraph, GraphUtils.printGraph(Resolution.create(
                mBasicResolution.rootDependency(), actual)));
    }

    @Test
    public void testExcludes_RemoveMatchesSubString() {
        Collection<Dependency> actual = ExcludesFilter.filterDependencies(mBasicResolution.allResolvedDependencies(),
                Collections.singletonList("net.evendanan:inner-inner2"));

        Assert.assertEquals(mBasicResolution.allResolvedDependencies().size() - 1, actual.size());
        Assert.assertTrue(actual.stream()
                .map(Dependency::mavenCoordinate)
                .noneMatch(mvn -> mvn.groupId().equals("net.evendanan") && mvn.artifactId().equals("inner-inner2")));

        Assert.assertEquals("" +
                        "  net.evendanan:dep1:0.1\n" +
                        "    net.evendanan:inner1:0.1\n" +
                        "      net.evendanan:inner-inner1:0.1\n",
                GraphUtils.printGraph(Resolution.create(
                        mBasicResolution.rootDependency(),
                        actual)));
    }

    @Test
    public void testExcludes_RemoveMatchesSubTree() {
        Collection<Dependency> actual = ExcludesFilter.filterDependencies(mBasicResolution.allResolvedDependencies(),
                Collections.singletonList("net.evendanan:inner1"));

        Assert.assertEquals("  net.evendanan:dep1:0.1\n",
                GraphUtils.printGraph(Resolution.create(
                        mBasicResolution.rootDependency(),
                        actual)));
    }

    @Test
    public void testReturnsUnChangedCopyWhenDependenciesDoNotAffectEachOther() {
        Resolution secondResolution = Resolution.create(
                MavenCoordinate.create("com.evendanan", "dep1", "0.1", ""),
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("com.evendanan", "dep1", "0.1", ""))
                                .url("http://example.com/artifact20.jar")
                                .dependencies(Collections.singleton(MavenCoordinate.create("com.evendanan", "inner1", "0.1", "")))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("com.evendanan", "inner1", "0.1", ""))
                                .url("http://example.com/artifact21.jar")
                                .build()));

        Collection<Dependency> actual = new PinBreadthFirstVersionsMerger().mergeGraphs(
                Arrays.asList(mBasicResolution, secondResolution));

        Assert.assertEquals(
                mBasicResolution.allResolvedDependencies().size() + secondResolution.allResolvedDependencies().size(),
                actual.size());
        actual.forEach(dep -> Assert.assertTrue(
                containsDependency(mBasicResolution.allResolvedDependencies(), dep) ||
                        containsDependency(secondResolution.allResolvedDependencies(), dep)));
    }

    @Test
    public void testPinsTopLevelVersion() {
        Resolution secondResolution = Resolution.create(
                MavenCoordinate.create("com.evendanan", "dep1", "0.1", ""),
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("com.evendanan", "dep1", "0.1", ""))
                                .url("http://example.com/artifact20.jar")
                                .dependencies(Collections.singleton(MavenCoordinate.create("net.evendanan", "inner1",
                                        "0.2"/*this will be changed to "0.1"*/, "")))
                                .build(),
                        Dependency.builder()//this will be discarded
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.2", ""))
                                .url("http://example.com/artifact21.jar")
                                .build()));

        Collection<Dependency> actual = new PinBreadthFirstVersionsMerger().mergeGraphs(
                Arrays.asList(mBasicResolution, secondResolution));

        //only adds the root of the second resolution
        Assert.assertEquals(mBasicResolution.allResolvedDependencies().size() + 1, actual.size());

        Assert.assertTrue(actual.stream()
                .map(Dependency::mavenCoordinate)
                .noneMatch(mvn -> mvn.groupId().equals("net.evendanan") && mvn.artifactId().equals("inner1") && mvn.version().equals("0.2")));

        Assert.assertEquals("" +
                        "  com.evendanan:dep1:0.1\n" +
                        "    net.evendanan:inner1:0.1\n" +
                        "      net.evendanan:inner-inner1:0.1\n" +
                        "      net.evendanan:inner-inner2:0.1\n",
                GraphUtils.printGraph(Resolution.create(
                        secondResolution.rootDependency(),
                        actual)));
    }

    @Test
    public void testUseEmptyUrlDepIfNoOther() {
        Resolution resolution = Resolution.create(
                MavenCoordinate.create("com.evendanan", "dep1", "0.1", ""),
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("com.evendanan", "dep1", "0.1", ""))
                                .dependencies(Collections.singleton(MavenCoordinate.create("net.evendanan", "inner1", "0.2", "")))
                                .url("http://example.com/artifact.jar")
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.2", ""))
                                .build()));

        Collection<Dependency> mergedDependencies = new PinBreadthFirstVersionsMerger().mergeGraphs(Collections.singleton(resolution));

        Assert.assertTrue(mergedDependencies.stream()
                .filter(dependency -> dependency.url().isEmpty())
                .map(Dependency::mavenCoordinate)
                .anyMatch(mvn -> mvn.groupId().equals("net.evendanan") && mvn.artifactId().equals("inner1") && mvn.version().equals("0.2")));
    }

    @Test
    public void testDiscardEmptyUrlDependency() {
        Resolution resolution = Resolution.create(
                MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""))
                                .dependencies(Arrays.asList(
                                        MavenCoordinate.create("net.evendanan", "inner1", "0.1", ""),
                                        MavenCoordinate.create("net.evendanan", "inner2", "0.1", "")
                                ))
                                .url("http://example.com/artifact1.jar")
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.1", ""))
                                .url("http://example.com/artifact2.jar")
                                .dependencies(Arrays.asList(
                                        MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""),
                                        MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", "")
                                ))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", ""))
                                .url("http://example.com/artifact4.jar")
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner2", "0.1", ""))
                                .url("http://example.com/artifact2-inner2.jar")
                                .dependencies(Arrays.asList(
                                        MavenCoordinate.create("net.evendanan", "inner-inner1", "0.2", ""),
                                        MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", "")
                                ))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner-inner1", "0.2", ""))
                                .url("http://example.com/artifact1-inner1-v2.jar")
                                .build()));

        Collection<Dependency> mergedDependencies = new PinBreadthFirstVersionsMerger().mergeGraphs(Collections.singleton(resolution));

        Assert.assertTrue(mergedDependencies.stream()
                .noneMatch(dependency -> dependency.url().isEmpty()));
        Assert.assertTrue(mergedDependencies.stream()
                .map(Dependency::mavenCoordinate)
                .noneMatch(mvn -> mvn.groupId().equals("net.evendanan") && mvn.artifactId().equals("inner-inner1") && mvn.version().equals("0.1")));
    }

    @Test
    public void testReturnsUnchangedIfNoDuplicateDependencies() {
        Set<Dependency> actual = FilterDuplicateDependenciesEntries.filterDuplicateDependencies(mBasicResolution.allResolvedDependencies());

        Assert.assertEquals(mBasicResolution.allResolvedDependencies().size(), actual.size());
        actual.forEach(dep -> Assert.assertTrue(containsDependency(mBasicResolution.allResolvedDependencies(), dep)));

        Assert.assertEquals(mBasicResolutionGraph, GraphUtils.printGraph(Resolution.create(
                mBasicResolution.rootDependency(),
                actual)));
    }

    @Test
    public void testReturnsDeDupDuplicateResolvedDependencies() {
        List<Dependency> testData = new ArrayList<>(mBasicResolution.allResolvedDependencies());
        testData.add(testData.get(testData.size() - 1));
        Set<Dependency> actual = FilterDuplicateDependenciesEntries.filterDuplicateDependencies(testData);

        Assert.assertEquals(mBasicResolution.allResolvedDependencies().size(), actual.size());
        actual.forEach(dep -> Assert.assertTrue(containsDependency(mBasicResolution.allResolvedDependencies(), dep)));

        Assert.assertEquals(mBasicResolutionGraph, GraphUtils.printGraph(Resolution.create(
                mBasicResolution.rootDependency(),
                actual)));
    }

    @Test
    public void testReturnsDeDupDuplicateDependencyDownstreamDependencies() {
        Resolution resolution = Resolution.create(
                MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""))
                                .dependencies(Collections.singleton(MavenCoordinate.create("net.evendanan", "inner1", "0.1", "")))
                                .url("http://example.com/artifact1.jar")
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.1", ""))
                                .url("http://example.com/artifact2.jar")
                                .dependencies(Arrays.asList(
                                        MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""),
                                        MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""),
                                        MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", "")
                                ))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""))
                                .url("http://example.com/artifact3.jar")
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", ""))
                                .url("http://example.com/artifact4.jar")
                                .build()));

        Set<Dependency> actual = FilterDuplicateDependenciesEntries.filterDuplicateDependencies(resolution.allResolvedDependencies());

        Assert.assertEquals(mBasicResolution.allResolvedDependencies().size(), actual.size());
        actual.forEach(dep -> Assert.assertTrue(containsDependency(mBasicResolution.allResolvedDependencies(), dep)));

        Assert.assertEquals(mBasicResolutionGraph, GraphUtils.printGraph(Resolution.create(
                mBasicResolution.rootDependency(),
                actual)));
    }

    @Test
    public void testNamePrefixWithDependency() {
        final String prefix = "prefix___";
        DependencyToolsWithPrefix prefixing = new DependencyToolsWithPrefix(prefix);

        Dependency dependency = Dependency.builder()
                .mavenCoordinate(MavenCoordinate.create("group", "artifact", "1.0", ""))
                .build();

        Assert.assertTrue(prefixing.repositoryRuleName(dependency).startsWith(prefix));
        Assert.assertTrue(prefixing.targetName(dependency).startsWith(prefix));
    }

    @Test
    public void testNamePrefixWithMavenCoordinates() {
        final String prefix = "prefix___";
        DependencyToolsWithPrefix prefixing = new DependencyToolsWithPrefix(prefix);

        MavenCoordinate dependency = MavenCoordinate.create("group", "artifact", "1.0", "");

        Assert.assertTrue(prefixing.repositoryRuleName(dependency).startsWith(prefix));
        Assert.assertTrue(prefixing.targetName(dependency).startsWith(prefix));
    }

    @Test
    public void testClearSrcJar() {
        Resolution resolution = Resolution.create(
                MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""),
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "dep1", "0.1", ""))
                                .dependencies(Collections.singleton(MavenCoordinate.create("net.evendanan", "inner1", "0.1", "")))
                                .url("http://example.com/artifact1.jar")
                                .sourcesUrl("http://example.com/artifact1-src.jar")
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner1", "0.1", ""))
                                .url("http://example.com/artifact2.jar")
                                .dependencies(Arrays.asList(
                                        MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""),
                                        MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", "")
                                ))
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner-inner1", "0.1", ""))
                                .url("http://example.com/artifact3.jar")
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(MavenCoordinate.create("net.evendanan", "inner-inner2", "0.1", ""))
                                .url("http://example.com/artifact4.jar")
                                .sourcesUrl("http://example.com/artifact4-src.jar")
                                .build()));

        Assert.assertTrue(resolution.allResolvedDependencies().stream()
                .filter(dependency -> !dependency.sourcesUrl().isEmpty())
                .map(Dependency::mavenCoordinate)
                .anyMatch(mvn -> mvn.groupId().equals("net.evendanan") && mvn.artifactId().equals("inner-inner2")));

        Collection<Dependency> actual = ClearSrcJarAttribute.clearSrcJar(resolution.allResolvedDependencies());

        Assert.assertTrue(actual.stream()
                .allMatch(dependency -> dependency.sourcesUrl().isEmpty()));
    }

    private static boolean containsDependency(Collection<Dependency> list, Dependency dependency) {
        return list.stream()
                .anyMatch(dep -> dep.mavenCoordinate().equals(dependency.mavenCoordinate()) &&
                        dep.url().equals(dependency.url()) &&
                        dep.sourcesUrl().equals(dependency.sourcesUrl()) &&
                        dep.javadocUrl().equals(dependency.javadocUrl()) &&
                        listEquals(dep.licenses(), dependency.licenses()) &&
                        listEquals(dep.dependencies(), dependency.dependencies()) &&
                        listEquals(dep.exports(), dependency.exports()) &&
                        listEquals(dep.runtimeDependencies(), dependency.runtimeDependencies()));
    }

    private static <T> boolean listEquals(Collection<T> l1, Collection<T> l2) {
        return l1.size() == l2.size() &&
                l2.containsAll(l1);
    }
}