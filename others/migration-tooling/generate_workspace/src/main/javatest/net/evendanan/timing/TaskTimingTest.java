package net.evendanan.timing;

import static net.evendanan.timing.TimingDataTest.assertTimingData;

import org.junit.Test;

public class TaskTimingTest {

    private static class TestableTimer extends TaskTiming {

        private int currentTime = 10;

        @Override
        long getCurrentTime() {
            return currentTime;
        }
    }

    @Test
    public void testHappyPath() {
        TestableTimer timer = new TestableTimer();
        timer.start();
        timer.setTotalTasks(10);
        timer.currentTime++;
        assertTimingData(timer.taskDone(),
            10, 1, 10, 11, 9, 0.1f);

        timer.currentTime++;
        assertTimingData(timer.taskDone(),
            10, 2, 10, 12, 8, 0.2f);

        timer.currentTime++;
        assertTimingData(timer.taskDone(),
            10, 3, 10, 13, 7, 0.3f);

        timer.currentTime++;
        assertTimingData(timer.taskDone(),
            10, 4, 10, 14, 6, 0.4f);

        timer.currentTime++;
        assertTimingData(timer.taskDone(),
            10, 5, 10, 15, 5, 0.5f);

        timer.currentTime++;
        assertTimingData(timer.taskDone(),
            10, 6, 10, 16, 4, 0.6f);

        timer.currentTime++;
        assertTimingData(timer.taskDone(),
            10, 7, 10, 17, 3, 0.7f);

        timer.currentTime++;
        timer.currentTime++;
        timer.currentTime++;
        timer.currentTime++;
        timer.currentTime++;
        timer.currentTime++;
        timer.currentTime++;
        timer.currentTime++;
        timer.currentTime++;//longer task
        assertTimingData(timer.taskDone(),
            10, 8, 10, 26, 4, 0.8f);

        //quick task
        assertTimingData(timer.taskDone(),
            10, 9, 10, 26, 1, 0.9f);

        timer.currentTime++;
        timer.currentTime++;
        timer.currentTime++;//very long last task
        assertTimingData(timer.taskDone(),
            10, 10, 10, 29, 0, 1.0f);
    }
}
