package net.evendanan.bazel.mvn.merger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.evendanan.bazel.mvn.api.Dependency;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SourcesLocatorTest {

    private final URI dep1Uri = URI.create("https://example.com/repo/net/evendanan/dep1/1.0.0/dep1-1.0.0.jar");
    private final URI dep1UriSources = URI.create("https://example.com/repo/net/evendanan/dep1/1.0.0/dep1-1.0.0-sources.jar");
    private final URI dep2Uri = URI.create("https://example.com/repo/net/evendanan/dep2/1.0.0/dep2-1.0.0.jar");
    private final URI dep2UriSources = URI.create("https://example.com/repo/net/evendanan/dep2/1.0.0/dep2-1.0.0-sources.jar");
    private final URI dep3Uri = URI.create("https://example.com/repo/net/evendanan/dep3/2.0.0/dep3-2.0.0.aar");
    private final URI dep3UriSources = URI.create("https://example.com/repo/net/evendanan/dep3/2.0.0/dep3-2.0.0-sources.jar");
    private SourcesJarLocator mUnderTest;
    private FakeOpener mFakeOpener;
    private List<Dependency> mTestData;

    @Before
    public void setup() {
        mTestData = Arrays.asList(
                new Dependency("net.evendanan", "dep1", "1.0.0", "jar",
                        Collections.emptyList(), Collections.emptyList(), Collections.singleton(
                        new Dependency("net.evendanan", "dep2", "1.0.0", "jar",
                                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                                dep2Uri, URI.create(""), URI.create(""), Collections.emptyList())),
                        dep1Uri, URI.create(""), URI.create(""), Collections.emptyList()),
                new Dependency("net.evendanan", "dep3", "2.0.0", "aar",
                        Collections.singleton(
                                new Dependency("net.evendanan", "dep2", "1.0.0", "jar",
                                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                                        dep2Uri, URI.create(""), URI.create(""), Collections.emptyList())), Collections.emptyList(), Collections.emptyList(),
                        dep3Uri, URI.create(""), URI.create(""), Collections.emptyList()));

        mFakeOpener = new FakeOpener();
        mUnderTest = new SourcesJarLocator(mFakeOpener);
        Assert.assertTrue(mFakeOpener.buildsCounter.isEmpty());
    }

    @Test
    public void testAddsSourcesIfExists() throws Exception {
        final HttpURLConnection httpURLConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(200).when(httpURLConnection).getResponseCode();
        mFakeOpener.returnedConnections.put(dep1UriSources.toURL(), httpURLConnection);
        mFakeOpener.returnedConnections.put(dep2UriSources.toURL(), httpURLConnection);
        mFakeOpener.returnedConnections.put(dep3UriSources.toURL(), httpURLConnection);

        final List<Dependency> fixedDeps = new ArrayList<>(mUnderTest.fillSourcesAttribute(mTestData));

        Assert.assertEquals(dep1UriSources.toASCIIString(), fixedDeps.get(0).sourcesUrl().toASCIIString());
        Assert.assertEquals(dep3UriSources.toASCIIString(), fixedDeps.get(1).sourcesUrl().toASCIIString());

        final List<Dependency> deeperDeps = new ArrayList<>(fixedDeps.get(1).dependencies());

        Assert.assertEquals(dep2UriSources.toASCIIString(), deeperDeps.get(0).sourcesUrl().toASCIIString());
    }

    @Test
    public void testDoesNotAddOnException() throws Exception {
        final HttpURLConnection httpURLConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.doThrow(new IOException()).when(httpURLConnection).getResponseCode();
        mFakeOpener.returnedConnections.put(dep1Uri.toURL(), httpURLConnection);
        mFakeOpener.returnedConnections.put(dep2Uri.toURL(), httpURLConnection);
        mFakeOpener.returnedConnections.put(dep3Uri.toURL(), httpURLConnection);

        final List<Dependency> fixedDeps = new ArrayList<>(mUnderTest.fillSourcesAttribute(mTestData));

        Assert.assertEquals("", fixedDeps.get(0).sourcesUrl().toASCIIString());
        Assert.assertEquals("", fixedDeps.get(1).sourcesUrl().toASCIIString());

        final List<Dependency> deeperDeps = new ArrayList<>(fixedDeps.get(1).dependencies());

        Assert.assertEquals("", deeperDeps.get(0).sourcesUrl().toASCIIString());
    }

    @Test
    public void testDoesNotAddOnNone200ResponseCode() throws Exception {
        final HttpURLConnection httpURLConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(400).when(httpURLConnection).getResponseCode();
        mFakeOpener.returnedConnections.put(dep1Uri.toURL(), httpURLConnection);
        mFakeOpener.returnedConnections.put(dep2Uri.toURL(), httpURLConnection);
        mFakeOpener.returnedConnections.put(dep3Uri.toURL(), httpURLConnection);

        final List<Dependency> fixedDeps = new ArrayList<>(mUnderTest.fillSourcesAttribute(mTestData));

        Assert.assertEquals("", fixedDeps.get(0).sourcesUrl().toASCIIString());
        Assert.assertEquals("", fixedDeps.get(1).sourcesUrl().toASCIIString());

        final List<Dependency> deeperDeps = new ArrayList<>(fixedDeps.get(1).dependencies());

        Assert.assertEquals("", deeperDeps.get(0).sourcesUrl().toASCIIString());
    }

    @Test
    public void testDoesNotAddOnNullConnection() throws Exception {
        final List<Dependency> fixedDeps = new ArrayList<>(mUnderTest.fillSourcesAttribute(mTestData));

        Assert.assertEquals("", fixedDeps.get(0).sourcesUrl().toASCIIString());
        Assert.assertEquals("", fixedDeps.get(1).sourcesUrl().toASCIIString());
    }

    @Test
    public void testDoesNotAddOnOpenException() {
        mFakeOpener.openFailure = true;

        final List<Dependency> fixedDeps = new ArrayList<>(mUnderTest.fillSourcesAttribute(mTestData));

        Assert.assertEquals("", fixedDeps.get(0).sourcesUrl().toASCIIString());
        Assert.assertEquals("", fixedDeps.get(1).sourcesUrl().toASCIIString());

        final List<Dependency> deeperDeps = new ArrayList<>(fixedDeps.get(1).dependencies());

        Assert.assertEquals("", deeperDeps.get(0).sourcesUrl().toASCIIString());
    }

    @Test
    public void testOnlyQueriesURIOnce() throws Exception {
        final HttpURLConnection httpURLConnection1 = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(200).when(httpURLConnection1).getResponseCode();
        mFakeOpener.returnedConnections.put(dep1UriSources.toURL(), httpURLConnection1);
        final HttpURLConnection httpURLConnection2 = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(200).when(httpURLConnection2).getResponseCode();
        mFakeOpener.returnedConnections.put(dep2UriSources.toURL(), httpURLConnection2);
        final HttpURLConnection httpURLConnection3 = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(200).when(httpURLConnection3).getResponseCode();
        mFakeOpener.returnedConnections.put(dep3UriSources.toURL(), httpURLConnection3);

        final List<Dependency> fixedDeps = new ArrayList<>(mUnderTest.fillSourcesAttribute(mTestData));

        Assert.assertEquals(dep1UriSources.toASCIIString(), fixedDeps.get(0).sourcesUrl().toASCIIString());
        Assert.assertEquals(dep3UriSources.toASCIIString(), fixedDeps.get(1).sourcesUrl().toASCIIString());

        final List<Dependency> deeperDeps = new ArrayList<>(fixedDeps.get(1).dependencies());

        Assert.assertEquals(dep2UriSources.toASCIIString(), deeperDeps.get(0).sourcesUrl().toASCIIString());

        final List<Dependency> otherDeeperDeps = new ArrayList<>(fixedDeps.get(0).runtimeDependencies());

        Assert.assertEquals(dep2UriSources.toASCIIString(), otherDeeperDeps.get(0).sourcesUrl().toASCIIString());

        Assert.assertEquals(3, mFakeOpener.buildsCounter.size());
        mFakeOpener.buildsCounter.forEach((url, integer) -> Assert.assertEquals(1, integer.intValue()));

        Assert.assertEquals(3, mFakeOpener.returnedConnections.size());
        mFakeOpener.returnedConnections.forEach((url, connection) -> {
            try {
                Mockito.verify(connection).getResponseCode();
                Mockito.verify(connection).setRequestMethod("HEAD");
                Mockito.verifyNoMoreInteractions(connection);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static class FakeOpener implements SourcesJarLocator.ConnectionOpener {

        private final Map<URL, Integer> buildsCounter = new HashMap<>();
        private final Map<URL, HttpURLConnection> returnedConnections = new HashMap<>();

        private boolean openFailure = false;

        @Override
        public HttpURLConnection openUrlConnection(final URL url) throws IOException {
            buildsCounter.compute(url, (key, count) -> count==null ? 1:count + 1);

            if (openFailure) {
                throw new IOException("failed to open connection");
            }
            return returnedConnections.get(url);
        }
    }
}