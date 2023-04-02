package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class SourcesLocatorTest {

    private final String dep1Uri =
            "https://example.com/repo/net/evendanan/dep1/1.0.0/dep1-1.0.0.jar";
    private final String dep1UriSources =
            "https://example.com/repo/net/evendanan/dep1/1.0.0/dep1-1.0.0-sources.jar";
    private final String dep2Uri =
            "https://example.com/repo/net/evendanan/dep2/1.0.0/dep2-1.0.0.jar";
    private final String dep2UriSources =
            "https://example.com/repo/net/evendanan/dep2/1.0.0/dep2-1.0.0-sources.jar";
    private final String dep3Uri =
            "https://example.com/repo/net/evendanan/dep3/2.0.0/dep3-2.0.0.aar";
    private final String dep3UriSources =
            "https://example.com/repo/net/evendanan/dep3/2.0.0/dep3-2.0.0-sources.jar";
    private SourcesJarLocator mUnderTest;
    private FakeOpener mFakeOpener;
    private List<Dependency> mTestData;

    @Before
    public void setup() {
        mTestData =
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "dep1", "1.0.0", "jar"))
                                .dependencies(
                                        Collections.singleton(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "dep2", "1.0.0", "jar")))
                                .url(dep1Uri)
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "dep3", "2.0.0", "aar"))
                                .dependencies(
                                        Collections.singleton(
                                                MavenCoordinate.create(
                                                        "net.evendanan", "dep2", "1.0.0", "jar")))
                                .url(dep3Uri)
                                .build());

        mFakeOpener = new FakeOpener();
        mUnderTest = new SourcesJarLocator(mFakeOpener);
        Assert.assertTrue(mFakeOpener.buildsCounter.isEmpty());
    }

    @Test
    public void testAddsSourcesIfExists() throws Exception {
        final HttpURLConnection httpURLConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(200).when(httpURLConnection).getResponseCode();
        mFakeOpener.returnedConnections.put(new URL(dep1UriSources), httpURLConnection);
        mFakeOpener.returnedConnections.put(new URL(dep2UriSources), httpURLConnection);
        mFakeOpener.returnedConnections.put(new URL(dep3UriSources), httpURLConnection);

        final List<Dependency> fixedDeps =
                new ArrayList<>(mUnderTest.fillSourcesAttribute(mTestData));

        Assert.assertEquals(dep1UriSources, fixedDeps.get(0).sourcesUrl());
        Assert.assertEquals(dep3UriSources, fixedDeps.get(1).sourcesUrl());
    }

    @Test
    public void testDoesNotAddOnException() throws Exception {
        final HttpURLConnection httpURLConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.doThrow(new IOException()).when(httpURLConnection).getResponseCode();
        mFakeOpener.returnedConnections.put(new URL(dep1Uri), httpURLConnection);
        mFakeOpener.returnedConnections.put(new URL(dep2Uri), httpURLConnection);
        mFakeOpener.returnedConnections.put(new URL(dep3Uri), httpURLConnection);

        final List<Dependency> fixedDeps =
                new ArrayList<>(mUnderTest.fillSourcesAttribute(mTestData));

        Assert.assertEquals("", fixedDeps.get(0).sourcesUrl());
        Assert.assertEquals("", fixedDeps.get(1).sourcesUrl());
    }

    @Test
    public void testDoesNotAddOnNone200ResponseCode() throws Exception {
        final HttpURLConnection httpURLConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(400).when(httpURLConnection).getResponseCode();
        mFakeOpener.returnedConnections.put(new URL(dep1Uri), httpURLConnection);
        mFakeOpener.returnedConnections.put(new URL(dep2Uri), httpURLConnection);
        mFakeOpener.returnedConnections.put(new URL(dep3Uri), httpURLConnection);

        final List<Dependency> fixedDeps =
                new ArrayList<>(mUnderTest.fillSourcesAttribute(mTestData));

        Assert.assertEquals("", fixedDeps.get(0).sourcesUrl());
        Assert.assertEquals("", fixedDeps.get(1).sourcesUrl());
    }

    @Test
    public void testDoesNotAddOnNullConnection() throws Exception {
        final List<Dependency> fixedDeps =
                new ArrayList<>(mUnderTest.fillSourcesAttribute(mTestData));

        Assert.assertEquals("", fixedDeps.get(0).sourcesUrl());
        Assert.assertEquals("", fixedDeps.get(1).sourcesUrl());
    }

    @Test
    public void testDoesNotAddOnOpenException() {
        mFakeOpener.openFailure = true;

        final List<Dependency> fixedDeps =
                new ArrayList<>(mUnderTest.fillSourcesAttribute(mTestData));

        Assert.assertEquals("", fixedDeps.get(0).sourcesUrl());
        Assert.assertEquals("", fixedDeps.get(1).sourcesUrl());
    }

    @Test
    public void testOnlyQueriesURIOnce() throws Exception {
        final HttpURLConnection httpURLConnection1 = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(200).when(httpURLConnection1).getResponseCode();
        mFakeOpener.returnedConnections.put(new URL(dep1UriSources), httpURLConnection1);
        final HttpURLConnection httpURLConnection2 = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(200).when(httpURLConnection2).getResponseCode();
        mFakeOpener.returnedConnections.put(new URL(dep2UriSources), httpURLConnection2);
        final HttpURLConnection httpURLConnection3 = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(200).when(httpURLConnection3).getResponseCode();
        mFakeOpener.returnedConnections.put(new URL(dep3UriSources), httpURLConnection3);

        final List<Dependency> repeatedDeps =
                Arrays.asList(
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "dep1", "1.0.0", "jar"))
                                .url(dep1Uri)
                                .build(),
                        Dependency.builder()
                                .mavenCoordinate(
                                        MavenCoordinate.create(
                                                "net.evendanan", "dep1", "1.0.0", "jar"))
                                .url(dep1Uri)
                                .build());
        final List<Dependency> fixedDeps =
                new ArrayList<>(mUnderTest.fillSourcesAttribute(repeatedDeps));

        Assert.assertEquals(dep1UriSources, fixedDeps.get(0).sourcesUrl());
        Assert.assertEquals(dep1UriSources, fixedDeps.get(1).sourcesUrl());

        Assert.assertEquals(1, mFakeOpener.buildsCounter.size());
        mFakeOpener.buildsCounter.forEach(
                (url, integer) -> Assert.assertEquals(1, integer.intValue()));

        Mockito.verify(httpURLConnection1).getResponseCode();
        Mockito.verify(httpURLConnection1).setRequestMethod("HEAD");
        Mockito.verifyNoMoreInteractions(httpURLConnection1);

        Mockito.verifyZeroInteractions(httpURLConnection2, httpURLConnection3);
    }

    private static class FakeOpener
            implements net.evendanan.bazel.mvn.merger.SourcesJarLocator.ConnectionFactory {

        private final Map<URL, Integer> buildsCounter = new HashMap<>();
        private final Map<URL, HttpURLConnection> returnedConnections = new HashMap<>();

        private boolean openFailure = false;

        @Override
        public HttpURLConnection openUrlConnection(final URL url) throws IOException {
            buildsCounter.compute(url, (key, count) -> count == null ? 1 : count + 1);

            if (openFailure) {
                throw new IOException("failed to open connection");
            }
            return returnedConnections.get(url);
        }
    }
}
