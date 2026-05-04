package net.evendanan.timing;

import com.google.common.annotations.VisibleForTesting;

public class TaskTiming {

  private long startTime;
  private int completedTasks = 0;
  private int totalTasks = 0;

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

  public synchronized TimingData start(final int totalTasksCount) {
    startTime = getCurrentTime();
    completedTasks = 0;
    totalTasks = totalTasksCount;
    return generateTimingData();
  }

  public synchronized TimingData updateTotalTasks(final int totalTasksCount) {
    totalTasks = totalTasksCount;
    return generateTimingData();
  }

  public synchronized TimingData taskDone() {
    completedTasks++;
    return generateTimingData();
  }

  public synchronized TimingData finish() {
    return generateTimingData();
  }

  private TimingData generateTimingData() {
    final long totalTime = getCurrentTime();
    final long duration = totalTime - startTime;
    final float ratioOfDone = (totalTasks == 0) ? 0 : completedTasks / (float) totalTasks;
    final long estimatedTimeLeft =
        (ratioOfDone == 0) ? 0 : (long) (duration / ratioOfDone) - duration;

    return new TimingData(
        totalTasks, completedTasks, startTime, totalTime, estimatedTimeLeft, ratioOfDone);
  }

  @VisibleForTesting
  long getCurrentTime() {
    return System.currentTimeMillis();
  }
}
