package net.evendanan.bazel.mvn.merger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class VersionComparatorTest {

    private VersionComparator mUnderTest;

    @Before
    public void setup() throws Exception {
        mUnderTest = new VersionComparator();
    }

    @Test
    public void testFullNumbers() throws Exception {
        Assert.assertEquals(0, mUnderTest.compare("1.0.0", "1.0.0"));
        Assert.assertEquals(0, mUnderTest.compare("1.2.0", "1.2.0"));
        Assert.assertEquals(0, mUnderTest.compare("1.2.333", "1.2.333"));
        assertLater("1.0.1", "1.0.0");
        assertLater("2.0.1", "1.2.0");
        assertLater("2.2.1", "2.2.0");
        assertLater("2.2.1", "2.1.8");
        assertLater("0.2.1", "0.1.8");
        assertLater("0.2.12", "0.2.11");
        assertLater("0.2.12", "0.2.8");
        assertLater("0.2.12", "0.1.8");
        assertLater("0.2", "0.1");
        assertLater("0.2", "0.1.2");
        assertLater("0.2.1", "0.1");
        assertLater("0.2.1", "");
        assertLater("0.2.1", null);
    }

    @Test
    public void testDifferentSubNumbers() throws Exception {
        Assert.assertEquals(0, mUnderTest.compare("", ""));
        Assert.assertEquals(0, mUnderTest.compare("1", "1"));
        Assert.assertEquals(0, mUnderTest.compare("1.2", "1.2"));
        Assert.assertEquals(0, mUnderTest.compare("1.2.0", "1.2.0"));
        assertLater("1.0.0", "1.0");
        assertLater("1.0.1", "1.0");
        assertLater("1.2", "1.1.2");
        assertLater("1.2.1", "");
        assertLater("1.2.1", "1");
        assertLater("1.2.1", "1.2");
        assertLater("1.2", "1.1.2");
        assertLater("2", "1.1.2");
        assertLater("2.2.1.2", "");
        assertLater("2.2.1.2", "2");
        assertLater("2.2.1.2", "2.2");
        assertLater("2.2.1.2", "2.2.1");
    }

    @Test
    public void testSnapshotIsBest() throws Exception {
        Assert.assertEquals(0, mUnderTest.compare("1.0.0-SNAPSHOT", "1.0.0-SNAPSHOT"));
        assertLater("1.0.0", "1.0.0-SNAPSHOT");
        assertLater("2.0.1", "1.2.0-SNAPSHOT");
        assertLater("2.0.1-SNAPSHOT", "1.2.0");
        assertLater("2.0.1-SNAPSHOT", "1.2.0-SNAPSHOT");
        assertLater("2.0.1-SNAPSHOT", "2.0.1-something");
        assertLater("2.0.1-SNAPSHOT", "2.0.1-another");
        assertLater("2.0.1-SNAPSHOT", "2.0.1-alpha");
        assertLater("2.0.1-SNAPSHOT", "2.0.1-beta");
        assertLater("2.0.1-SNAPSHOT", "2.0.1-rc");
        assertLater("2.0.1-SNAPSHOT", "2.0.1-rc1");
    }

    @Test
    public void testReleasesCandidateRules() throws Exception {
        Assert.assertEquals(0, mUnderTest.compare("1.0.0-rc1", "1.0.0-rc1"));
        assertLater("2.0.1", "2.0.1-rc1");
        assertLater("2.0.1", "2.0.1-rc2");
        assertLater("2.0.1", "2.0.1-rc-2");
        assertLater("2.0.1-rc8", "2.0.1-rc");
        assertLater("2.0.1-rc8", "2.0.1-rc1");
        assertLater("2.0.1-rc8", "2.0.1-rc2");
        assertLater("2.0.1-rc2", "2.0.1-rc1");
        assertLater("2.0.1-rc-2", "2.0.1-rc-1");
        assertLater("1.1-rc-2", "1.0.1");
    }

    @Test
    public void testAlphaBetaRc() throws Exception {
        Assert.assertEquals(0, mUnderTest.compare("1.0.0-alpha", "1.0.0-alpha"));
        assertLater("2.0.1", "2.0.1-alpha");
        assertLater("2.0.1", "2.0.1beta");
        assertLater("2.0.1", "2.0.1-alpha");
        assertLater("2.0.1-beta", "2.0.1-alpha");
        assertLater("2.0.2-alpha", "2.0.1-beta");
        assertLater("2.0.2-alpha", "2.0.1-rc1");
        assertLater("1.1-alpha", "1.0.1");
    }

    @Test
    public void testLexi() throws Exception {
        Assert.assertEquals(0, mUnderTest.compare("something", "something"));
        assertLater("2.0.1", "2.0.1-something");
        assertLater("2.0.1", "2.0.1something");
        assertLater("2.0.1-CCCCC", "2.0.1-AA");
        assertLater("2.0.2-AAAA", "2.0.1-CCCC");
    }

    private void assertLater(String later, String previous) {
        Assert.assertEquals(
                "later " + later + " vs previous " + previous,
                1,
                mUnderTest.compare(later, previous));
    }
}
