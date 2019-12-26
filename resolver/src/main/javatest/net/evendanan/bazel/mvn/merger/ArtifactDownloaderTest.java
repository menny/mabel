package net.evendanan.bazel.mvn.merger;

import com.google.common.base.Charsets;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ArtifactDownloaderTest {

    private TestOpener mTestOpener;
    private Dependency mDependency;
    private File expectedOutputFile;
    private ArtifactDownloader mUnderTest;

    @Before
    public void setup() throws Exception {
        mTestOpener = new TestOpener();
        final File artifactDownloaderFolder = File.createTempFile("ArtifactDownloaderTest", "test");
        if (!artifactDownloaderFolder.delete()) {
            throw new IOException("Failed to setup temp folder for test");
        }
        if (!artifactDownloaderFolder.mkdir()) {
            throw new IOException("Failed to create temp folder for test");
        }
        mUnderTest = new ArtifactDownloader(mTestOpener, artifactDownloaderFolder, DependencyTools.DEFAULT);

        mDependency = Dependency.newBuilder()
                .setGroupId("group")
                .setArtifactId("artifact")
                .setVersion("1.1")
                .setPackaging("jar")
                .setUrl("https://example.com/temp.jar")
                .build();
        expectedOutputFile = new File(artifactDownloaderFolder, DependencyTools.DEFAULT.repositoryRuleName(mDependency));
    }

    @Test
    public void testDoesNotDownloadIfLocalFileExists() throws Exception {
        Assert.assertTrue("expected file already exists!", expectedOutputFile.createNewFile());

        final URI localUri = mUnderTest.getLocalUriForDependency(mDependency);

        Assert.assertTrue(expectedOutputFile.exists());
        Assert.assertEquals(expectedOutputFile.getAbsolutePath(), new File(localUri).getAbsolutePath());

        Assert.assertFalse(mTestOpener.mAccessCounters.containsKey(new URL(mDependency.getUrl())));
    }

    @Test
    public void testDownloadIfLocalFileDoesNotExist() throws Exception {
        Assert.assertFalse("expected file already exists!", expectedOutputFile.exists());

        final URI localUri = mUnderTest.getLocalUriForDependency(mDependency);

        Assert.assertTrue(expectedOutputFile.exists());
        Assert.assertEquals(expectedOutputFile.getAbsolutePath(), new File(localUri).getAbsolutePath());

        Assert.assertArrayEquals(TestOpener.TEST_OUTPUT.getBytes(), Files.readAllBytes(expectedOutputFile.toPath()));
    }

    @Test
    public void testDownloadIfLocalFileDoesNotExistButOnlyOnce() throws Exception {
        Assert.assertFalse("expected file already exists!", expectedOutputFile.exists());

        mUnderTest.getLocalUriForDependency(mDependency);
        final URI localUri2 = mUnderTest.getLocalUriForDependency(mDependency);

        Assert.assertEquals(expectedOutputFile.getAbsolutePath(), new File(localUri2).getAbsolutePath());

        Assert.assertArrayEquals(TestOpener.TEST_OUTPUT.getBytes(), Files.readAllBytes(expectedOutputFile.toPath()));
        Assert.assertEquals(Integer.valueOf(1), mTestOpener.mAccessCounters.get(new URL(mDependency.getUrl())));
    }

    private static class TestOpener implements ArtifactDownloader.ConnectionOpener {
        private static String TEST_OUTPUT = "testing 1 2 3";
        private Map<URL, Integer> mAccessCounters = new HashMap<>();

        @Override
        public InputStream openInputStream(final URL url) throws IOException {
            mAccessCounters.compute(url, (urlKey, currentValue) -> {
                if (currentValue == null) return 1;
                else return currentValue + 1;
            });

            return new ByteArrayInputStream(TEST_OUTPUT.getBytes(Charsets.UTF_8));
        }
    }
}