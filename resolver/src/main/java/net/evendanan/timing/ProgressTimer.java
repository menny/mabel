package net.evendanan.timing;

import java.util.Locale;

public class ProgressTimer {
  private final TaskTiming timer = new TaskTiming();
  private final String title;
  private final String progressText;

  public ProgressTimer(int tasksCount, String title, String progressText) {
    this.title = title;
    this.progressText = progressText;
    this.timer.start(tasksCount);
  }

  public void taskDone(String taskName) {
    final TimingData timingData = timer.taskDone();
    final String estimatedTimeLeft;
    if (timingData.doneTasks >= 3) {
      estimatedTimeLeft =
          String.format(
              Locale.ROOT, ", %s left", TaskTiming.humanReadableTime(timingData.estimatedTimeLeft));
    } else {
      estimatedTimeLeft = "";
    }
    report(
        progressText,
        timingData.doneTasks,
        timingData.totalTasks,
        100 * timingData.ratioOfDone,
        estimatedTimeLeft,
        taskName);
  }

  public void finish() {
    TimingData finish = timer.finish();
    report("Finished. %s", TaskTiming.humanReadableTime(finish.totalTime - finish.startTime));
  }

  private void report(String text, Object... args) {
    String msg = String.format(Locale.ROOT, text, args);
    System.out.println(String.format(Locale.ROOT, "[%s] %s", title, msg));
  }
}
