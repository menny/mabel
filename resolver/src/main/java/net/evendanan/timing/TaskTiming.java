package net.evendanan.timing;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskTiming {

  private long startTime;
  private final AtomicInteger completedTasks = new AtomicInteger(0);
  private final AtomicInteger totalTasks = new AtomicInteger(0);

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

  public TimingData start(final int totalTasksCount) {
    startTime = getCurrentTime();
    completedTasks.set(0);
    totalTasks.set(totalTasksCount);
    return generateTimingData();
  }

  public TimingData updateTotalTasks(final int totalTasksCount) {
    totalTasks.set(totalTasksCount);
    return generateTimingData();
  }

  public TimingData taskDone() {
    completedTasks.incrementAndGet();
    return generateTimingData();
  }

  public TimingData finish() {
    return generateTimingData();
  }

  private TimingData generateTimingData() {
    final long totalTime = getCurrentTime();
    final long duration = totalTime - startTime;
    final int completed = completedTasks.get();
    final int total = totalTasks.get();
    final float ratioOfDone = completed / (float) total;
    final long estimatedTimeLeft = (long) (duration / ratioOfDone) - duration;

    return new TimingData(total, completed, startTime, totalTime, estimatedTimeLeft, ratioOfDone);
  }

  @VisibleForTesting
  long getCurrentTime() {
    return System.currentTimeMillis();
  }
}
