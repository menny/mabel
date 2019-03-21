package net.evendanan.bazel.mvn.merger;

import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.evendanan.bazel.mvn.api.Dependency;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ArtifactDownloaderTest {

    private TestOpenner mTestOpenner;
    private Dependency mDependency;
    private File expectedOutputFile;
    private ArtifactDownloader mUnderTest;

    @Before
    public void setup() throws Exception {
        mTestOpenner = new TestOpenner();
        final File artifactDownloaderFolder = File.createTempFile("ArtifactDownloaderTest", "test");
        if (!artifactDownloaderFolder.delete()) {
            throw new IOException("Failed to setup temp folder for test");
        }
        if (!artifactDownloaderFolder.mkdir()) {
            throw new IOException("Failed to create temp folder for test");
        }
        mUnderTest = new ArtifactDownloader(mTestOpenner, artifactDownloaderFolder);

        mDependency = new Dependency("group", "artifact", "1.1", "jar", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                URI.create("https://example.com/temp.jar"), URI.create(""), URI.create(""), Collections.emptyList());
        expectedOutputFile = new File(artifactDownloaderFolder, mDependency.repositoryRuleName());
    }

    @Test
    public void testDoesNotDownloadIfLocalFileExists() throws Exception {
        Assert.assertTrue("expected file already exists!", expectedOutputFile.createNewFile());

        final URI localUri = mUnderTest.getLocalUriForDependency(mDependency);

        Assert.assertTrue(expectedOutputFile.exists());
        Assert.assertEquals(expectedOutputFile.getAbsolutePath(), new File(localUri).getAbsolutePath());

        Assert.assertFalse(mTestOpenner.mAccessCounters.containsKey(mDependency.url().toURL()));
    }

    @Test
    public void testDownloadIfLocalFileDoesNotExist() throws Exception {
        Assert.assertFalse("expected file already exists!", expectedOutputFile.exists());

        final URI localUri = mUnderTest.getLocalUriForDependency(mDependency);

        Assert.assertTrue(expectedOutputFile.exists());
        Assert.assertEquals(expectedOutputFile.getAbsolutePath(), new File(localUri).getAbsolutePath());

        Assert.assertArrayEquals(TestOpenner.TEST_OUTPUT.getBytes(), Files.readAllBytes(expectedOutputFile.toPath()));
    }

    @Test
    public void testDownloadIfLocalFileDoesNotExistButOnlyOnce() throws Exception {
        Assert.assertFalse("expected file already exists!", expectedOutputFile.exists());

        mUnderTest.getLocalUriForDependency(mDependency);
        final URI localUri2 = mUnderTest.getLocalUriForDependency(mDependency);

        Assert.assertEquals(expectedOutputFile.getAbsolutePath(), new File(localUri2).getAbsolutePath());

        Assert.assertArrayEquals(TestOpenner.TEST_OUTPUT.getBytes(), Files.readAllBytes(expectedOutputFile.toPath()));
        Assert.assertEquals(Integer.valueOf(1), mTestOpenner.mAccessCounters.get(mDependency.url().toURL()));
    }

    private static class TestOpenner implements ArtifactDownloader.ConnectionOpener {
        private static String TEST_OUTPUT = "testing 1 2 3";
        private Map<URL, Integer> mAccessCounters = new HashMap<>();

        @Override
        public InputStream openInputStream(final URL url) throws IOException {
            mAccessCounters.compute(url, (urlKey, currentValue) -> {
                if (currentValue==null) return 1;
                else return currentValue + 1;
            });

            return new ByteArrayInputStream(TEST_OUTPUT.getBytes(Charsets.UTF_8));
        }
    }
}