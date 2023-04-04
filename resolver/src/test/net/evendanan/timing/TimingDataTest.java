package net.evendanan.timing;

import org.junit.Assert;
import org.junit.Test;

public class TimingDataTest {

    static void assertTimingData(
            final TimingData data,
            int totalTasks,
            int doneTasks,
            long startTime,
            long totalTime,
            long estimatedTimeLeft,
            float ratioOfDone) {
        Assert.assertEquals(totalTasks, data.totalTasks);
        Assert.assertEquals(doneTasks, data.doneTasks);
        Assert.assertEquals(startTime, data.startTime);
        Assert.assertEquals(totalTime, data.totalTime);
        Assert.assertEquals(estimatedTimeLeft, data.estimatedTimeLeft);
        Assert.assertEquals(ratioOfDone, data.ratioOfDone, 0.001f);
    }

    @Test
    public void testCtor() {
        TimingData data = new TimingData(2, 1, 10, 20, 15, 0.7f);
        assertTimingData(data, 2, 1, 10, 20, 15, 0.7f);
    }
}
