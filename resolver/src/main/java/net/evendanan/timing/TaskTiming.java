package net.evendanan.timing;

import com.google.common.annotations.VisibleForTesting;

public class TaskTiming {

    private long startTime;
    private int completedTasks;
    private int totalTasks;

    public static String humanReadableTime(long milliseconds) {
        final long secondsInMilli = 1000;
        final long minutesInMilli = secondsInMilli * 60;

        String timeString = "";

        long elapsedMinutes = milliseconds / minutesInMilli;
        milliseconds = milliseconds % minutesInMilli;
        if (elapsedMinutes > 0) {
            timeString += elapsedMinutes + " minutes and ";
        }
        long elapsedSeconds = milliseconds / secondsInMilli;
        timeString += elapsedSeconds + " seconds";

        return timeString;
    }

    public void start() {
        startTime = getCurrentTime();
        completedTasks = 0;
        totalTasks = 1;
    }

    public void setTotalTasks(final int totalTasksCount) {
        totalTasks = totalTasksCount;
    }

    public TimingData taskDone() {
        completedTasks++;
        final long totalTime = getCurrentTime();
        final long duration = totalTime - startTime;
        final float ratioOfDone = completedTasks / (float) totalTasks;
        final long estimatedTimeLeft = (long) (duration / ratioOfDone) - duration;

        return new TimingData(totalTasks, completedTasks, startTime, totalTime, estimatedTimeLeft, ratioOfDone);
    }

    @VisibleForTesting
    long getCurrentTime() {
        return System.currentTimeMillis();
    }
}
