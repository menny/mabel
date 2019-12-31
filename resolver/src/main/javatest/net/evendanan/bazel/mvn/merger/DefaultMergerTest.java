package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.MavenCoordinate;
import net.evendanan.bazel.mvn.api.Resolution;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class DefaultMergerTest {

    private Resolution mBasicResolution;
    private String mBasicResolutionGraph;

    @Before
    public void setup() {
        mBasicResolution = Resolution.newBuilder()
                .setRootDependency(MavenCoordinate.newBuilder()
                        .setGroupId("net.evendanan")
                        .setArtifactId("dep1")
                        .setVersion("0.1")
                        .build())
                .addAllAllResolvedDependencies(Arrays.asList(
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("dep1")
                                        .setVersion("0.1")
                                        .build())
                                .addAllDependencies(Collections.singleton(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.1")
                                        .build()))
                                .setUrl("http://example.com/artifact1.jar")
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact2.jar")
                                .addAllDependencies(Arrays.asList(
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner1")
                                                .setVersion("0.1")
                                                .build(),
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner2")
                                                .setVersion("0.1")
                                                .build()
                                ))
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner-inner1")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact3.jar")
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner-inner2")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact4.jar")
                                .build()))
                .build();
        mBasicResolutionGraph = GraphUtils.printGraph(mBasicResolution);
    }

    @Test
    public void testExcludes_NoRemovalIfEmptyExclude() {
        Collection<Dependency> actual = ExcludesFilter.filterDependencies(mBasicResolution.getAllResolvedDependenciesList(),
                Collections.emptyList());

        Assert.assertEquals(mBasicResolution.getAllResolvedDependenciesCount(), actual.size());
        actual.forEach(dep -> Assert.assertTrue(mBasicResolution.getAllResolvedDependenciesList().contains(dep)));

        Assert.assertEquals(mBasicResolutionGraph, GraphUtils.printGraph(Resolution.newBuilder()
                .setRootDependency(mBasicResolution.getRootDependency())
                .addAllAllResolvedDependencies(actual)
                .build()));
    }

    @Test
    public void testExcludes_NoRemovalIfNoMatches() {
        Collection<Dependency> actual = ExcludesFilter.filterDependencies(mBasicResolution.getAllResolvedDependenciesList(),
                Arrays.asList("some:other:1.2", "net.evendanan:dep1:333.00", "net.evendanan:dep5"));

        Assert.assertEquals(mBasicResolution.getAllResolvedDependenciesCount(), actual.size());
        actual.forEach(dep -> Assert.assertTrue(mBasicResolution.getAllResolvedDependenciesList().contains(dep)));

        Assert.assertEquals(mBasicResolutionGraph, GraphUtils.printGraph(Resolution.newBuilder()
                .setRootDependency(mBasicResolution.getRootDependency())
                .addAllAllResolvedDependencies(actual)
                .build()));
    }

    @Test
    public void testExcludes_RemoveMatchesSubString() {
        Collection<Dependency> actual = ExcludesFilter.filterDependencies(mBasicResolution.getAllResolvedDependenciesList(),
                Collections.singletonList("net.evendanan:inner-inner2"));

        Assert.assertEquals(mBasicResolution.getAllResolvedDependenciesCount() - 1, actual.size());
        Assert.assertTrue(actual.stream()
                .map(Dependency::getMavenCoordinate)
                .noneMatch(mvn -> mvn.getGroupId().equals("net.evendanan") && mvn.getArtifactId().equals("inner-inner2")));

        Assert.assertEquals("" +
                        "  net.evendanan:dep1:0.1\n" +
                        "    net.evendanan:inner1:0.1\n" +
                        "      net.evendanan:inner-inner1:0.1\n",
                GraphUtils.printGraph(Resolution.newBuilder()
                        .setRootDependency(mBasicResolution.getRootDependency())
                        .addAllAllResolvedDependencies(actual)
                        .build()));
    }

    @Test
    public void testExcludes_RemoveMatchesSubTree() {
        Collection<Dependency> actual = ExcludesFilter.filterDependencies(mBasicResolution.getAllResolvedDependenciesList(),
                Collections.singletonList("net.evendanan:inner1"));

        Assert.assertEquals("  net.evendanan:dep1:0.1\n",
                GraphUtils.printGraph(Resolution.newBuilder()
                        .setRootDependency(mBasicResolution.getRootDependency())
                        .addAllAllResolvedDependencies(actual)
                        .build()));
    }

    @Test
    public void testReturnsUnChangedCopyWhenDependenciesDoNotAffectEachOther() {
        Resolution secondResolution = Resolution.newBuilder()
                .setRootDependency(MavenCoordinate.newBuilder()
                        .setGroupId("com.evendanan")
                        .setArtifactId("dep1")
                        .setVersion("0.1")
                        .build())
                .addAllAllResolvedDependencies(Arrays.asList(
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("com.evendanan")
                                        .setArtifactId("dep1")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact20.jar")
                                .addAllDependencies(Collections.singleton(MavenCoordinate.newBuilder()
                                        .setGroupId("com.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.1")
                                        .build()))
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("com.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact21.jar")
                                .build()))
                .build();

        Collection<Dependency> actual = new PinBreadthFirstVersionsMerger().mergeGraphs(
                Arrays.asList(mBasicResolution, secondResolution));

        Assert.assertEquals(
                mBasicResolution.getAllResolvedDependenciesCount() + secondResolution.getAllResolvedDependenciesCount(),
                actual.size());
        actual.forEach(dep -> Assert.assertTrue(
                mBasicResolution.getAllResolvedDependenciesList().contains(dep) ||
                        secondResolution.getAllResolvedDependenciesList().contains(dep)));
    }

    @Test
    public void testPinsTopLevelVersion() {
        Resolution secondResolution = Resolution.newBuilder()
                .setRootDependency(MavenCoordinate.newBuilder()
                        .setGroupId("com.evendanan")
                        .setArtifactId("dep1")
                        .setVersion("0.1")
                        .build())
                .addAllAllResolvedDependencies(Arrays.asList(
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("com.evendanan")
                                        .setArtifactId("dep1")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact20.jar")
                                .addAllDependencies(Collections.singleton(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.2")//this will be changed to "0.1"
                                        .build()))
                                .build(),
                        Dependency.newBuilder()//this will be discarded
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.2")
                                        .build())
                                .setUrl("http://example.com/artifact21.jar")
                                .build()))
                .build();

        Collection<Dependency> actual = new PinBreadthFirstVersionsMerger().mergeGraphs(
                Arrays.asList(mBasicResolution, secondResolution));

        //only adds the root of the second resolution
        Assert.assertEquals(mBasicResolution.getAllResolvedDependenciesCount() + 1, actual.size());

        Assert.assertTrue(actual.stream()
                .map(Dependency::getMavenCoordinate)
                .noneMatch(mvn -> mvn.getGroupId().equals("net.evendanan") && mvn.getArtifactId().equals("inner1") && mvn.getVersion().equals("0.2")));

        Assert.assertEquals("" +
                        "  com.evendanan:dep1:0.1\n" +
                        "    net.evendanan:inner1:0.1\n" +
                        "      net.evendanan:inner-inner1:0.1\n" +
                        "      net.evendanan:inner-inner2:0.1\n",
                GraphUtils.printGraph(Resolution.newBuilder()
                        .setRootDependency(secondResolution.getRootDependency())
                        .addAllAllResolvedDependencies(actual)
                        .build()));
    }

    @Test
    public void testUseEmptyUrlDepIfNoOther() {
        Resolution resolution = Resolution.newBuilder()
                .setRootDependency(MavenCoordinate.newBuilder()
                        .setGroupId("com.evendanan")
                        .setArtifactId("dep1")
                        .setVersion("0.1")
                        .build())
                .addAllAllResolvedDependencies(Arrays.asList(
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("com.evendanan")
                                        .setArtifactId("dep1")
                                        .setVersion("0.1")
                                        .build())
                                .addAllDependencies(Collections.singleton(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.2")
                                        .build()))
                                .setUrl("http://example.com/artifact.jar")
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.2")
                                        .build())
                                .build()))
                .build();

        Collection<Dependency> mergedDependencies = new PinBreadthFirstVersionsMerger().mergeGraphs(Collections.singleton(resolution));

        Assert.assertTrue(mergedDependencies.stream()
                .filter(dependency -> dependency.getUrl().isEmpty())
                .map(Dependency::getMavenCoordinate)
                .anyMatch(mvn -> mvn.getGroupId().equals("net.evendanan") && mvn.getArtifactId().equals("inner1") && mvn.getVersion().equals("0.2")));
    }

    @Test
    public void testDiscardEmptyUrlDependency() {
        Resolution resolution = Resolution.newBuilder()
                .setRootDependency(MavenCoordinate.newBuilder()
                        .setGroupId("net.evendanan")
                        .setArtifactId("dep1")
                        .setVersion("0.1")
                        .build())
                .addAllAllResolvedDependencies(Arrays.asList(
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("dep1")
                                        .setVersion("0.1")
                                        .build())
                                .addAllDependencies(Arrays.asList(
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner1")
                                                .setVersion("0.1")
                                                .build(),
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner2")
                                                .setVersion("0.1")
                                                .build()
                                ))
                                .setUrl("http://example.com/artifact1.jar")
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact2.jar")
                                .addAllDependencies(Arrays.asList(
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner1")
                                                .setVersion("0.1")
                                                .build(),
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner2")
                                                .setVersion("0.1")
                                                .build()
                                ))
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner-inner1")
                                        .setVersion("0.1")
                                        .build())
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner-inner2")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact4.jar")
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner2")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact2-inner2.jar")
                                .addAllDependencies(Arrays.asList(
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner1")
                                                .setVersion("0.2")
                                                .build(),
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner2")
                                                .setVersion("0.1")
                                                .build()
                                ))
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner-inner1")
                                        .setVersion("0.2")
                                        .build())
                                .setUrl("http://example.com/artifact1-inner1-v2.jar")
                                .build()))
                .build();

        Collection<Dependency> mergedDependencies = new PinBreadthFirstVersionsMerger().mergeGraphs(Collections.singleton(resolution));

        Assert.assertTrue(mergedDependencies.stream()
                .noneMatch(dependency -> dependency.getUrl().isEmpty()));
        Assert.assertTrue(mergedDependencies.stream()
                .map(Dependency::getMavenCoordinate)
                .noneMatch(mvn -> mvn.getGroupId().equals("net.evendanan") && mvn.getArtifactId().equals("inner-inner1") && mvn.getVersion().equals("0.1")));
    }

    @Test
    public void testReturnsUnchangedIfNoDuplicateDependencies() {
        Set<Dependency> actual = FilterDuplicateDependenciesEntries.filterDuplicateDependencies(mBasicResolution.getAllResolvedDependenciesList());

        Assert.assertEquals(mBasicResolution.getAllResolvedDependenciesCount(), actual.size());
        actual.forEach(dep -> Assert.assertTrue(mBasicResolution.getAllResolvedDependenciesList().contains(dep)));

        Assert.assertEquals(mBasicResolutionGraph, GraphUtils.printGraph(Resolution.newBuilder()
                .setRootDependency(mBasicResolution.getRootDependency())
                .addAllAllResolvedDependencies(actual)
                .build()));
    }

    @Test
    public void testReturnsDeDupDuplicateResolvedDependencies() {
        List<Dependency> testData = new ArrayList<>(mBasicResolution.getAllResolvedDependenciesList());
        testData.add(testData.get(testData.size() - 1));
        Set<Dependency> actual = FilterDuplicateDependenciesEntries.filterDuplicateDependencies(testData);

        Assert.assertEquals(mBasicResolution.getAllResolvedDependenciesCount(), actual.size());
        actual.forEach(dep -> Assert.assertTrue(mBasicResolution.getAllResolvedDependenciesList().contains(dep)));

        Assert.assertEquals(mBasicResolutionGraph, GraphUtils.printGraph(Resolution.newBuilder()
                .setRootDependency(mBasicResolution.getRootDependency())
                .addAllAllResolvedDependencies(actual)
                .build()));
    }

    @Test
    public void testReturnsDeDupDuplicateDependencyDownstreamDependencies() {
        Resolution resolution = Resolution.newBuilder()
                .setRootDependency(MavenCoordinate.newBuilder()
                        .setGroupId("net.evendanan")
                        .setArtifactId("dep1")
                        .setVersion("0.1")
                        .build())
                .addAllAllResolvedDependencies(Arrays.asList(
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("dep1")
                                        .setVersion("0.1")
                                        .build())
                                .addAllDependencies(Collections.singleton(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.1")
                                        .build()))
                                .setUrl("http://example.com/artifact1.jar")
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact2.jar")
                                .addAllDependencies(Arrays.asList(
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner1")
                                                .setVersion("0.1")
                                                .build(),
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner1")
                                                .setVersion("0.1")
                                                .build(),
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner2")
                                                .setVersion("0.1")
                                                .build()
                                ))
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner-inner1")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact3.jar")
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner-inner2")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact4.jar")
                                .build()))
                .build();

        Set<Dependency> actual = FilterDuplicateDependenciesEntries.filterDuplicateDependencies(resolution.getAllResolvedDependenciesList());

        Assert.assertEquals(mBasicResolution.getAllResolvedDependenciesCount(), actual.size());
        actual.forEach(dep -> Assert.assertTrue(mBasicResolution.getAllResolvedDependenciesList().contains(dep)));

        Assert.assertEquals(mBasicResolutionGraph, GraphUtils.printGraph(Resolution.newBuilder()
                .setRootDependency(mBasicResolution.getRootDependency())
                .addAllAllResolvedDependencies(actual)
                .build()));
    }

    @Test
    public void testNamePrefixWithDependency() {
        final String prefix = "prefix___";
        DependencyToolsWithPrefix prefixing = new DependencyToolsWithPrefix(prefix);

        Dependency dependency = Dependency.newBuilder().setMavenCoordinate(
                MavenCoordinate.newBuilder()
                        .setGroupId("group").setArtifactId("artifact").setVersion("1.0")
                        .build()
        ).build();

        Assert.assertTrue(prefixing.repositoryRuleName(dependency).startsWith(prefix));
        Assert.assertTrue(prefixing.targetName(dependency).startsWith(prefix));
    }

    @Test
    public void testNamePrefixWithMavenCoordinates() {
        final String prefix = "prefix___";
        DependencyToolsWithPrefix prefixing = new DependencyToolsWithPrefix(prefix);

        MavenCoordinate dependency = MavenCoordinate.newBuilder()
                .setGroupId("group").setArtifactId("artifact").setVersion("1.0")
                .build();

        Assert.assertTrue(prefixing.repositoryRuleName(dependency).startsWith(prefix));
        Assert.assertTrue(prefixing.targetName(dependency).startsWith(prefix));
    }

    @Test
    public void testClearSrcJar() {
        Resolution resolution = Resolution.newBuilder()
                .setRootDependency(MavenCoordinate.newBuilder()
                        .setGroupId("net.evendanan")
                        .setArtifactId("dep1")
                        .setVersion("0.1")
                        .build())
                .addAllAllResolvedDependencies(Arrays.asList(
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("dep1")
                                        .setVersion("0.1")
                                        .build())
                                .addAllDependencies(Collections.singleton(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.1")
                                        .build()))
                                .setUrl("http://example.com/artifact1.jar")
                                .setSourcesUrl("http://example.com/artifact1-src.jar")
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner1")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact2.jar")
                                .addAllDependencies(Arrays.asList(
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner1")
                                                .setVersion("0.1")
                                                .build(),
                                        MavenCoordinate.newBuilder()
                                                .setGroupId("net.evendanan")
                                                .setArtifactId("inner-inner2")
                                                .setVersion("0.1")
                                                .build()
                                ))
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner-inner1")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact3.jar")
                                .build(),
                        Dependency.newBuilder()
                                .setMavenCoordinate(MavenCoordinate.newBuilder()
                                        .setGroupId("net.evendanan")
                                        .setArtifactId("inner-inner2")
                                        .setVersion("0.1")
                                        .build())
                                .setUrl("http://example.com/artifact4.jar")
                                .setSourcesUrl("http://example.com/artifact4-src.jar")
                                .build()))
                .build();

        Assert.assertTrue(resolution.getAllResolvedDependenciesList().stream()
                .filter(dependency -> !dependency.getSourcesUrl().isEmpty())
                .map(Dependency::getMavenCoordinate)
                .anyMatch(mvn -> mvn.getGroupId().equals("net.evendanan") && mvn.getArtifactId().equals("inner-inner2")));

        Collection<Dependency> actual = ClearSrcJarAttribute.clearSrcJar(resolution.getAllResolvedDependenciesList());

        Assert.assertTrue(actual.stream()
                .allMatch(dependency -> dependency.getSourcesUrl().isEmpty()));
    }
}