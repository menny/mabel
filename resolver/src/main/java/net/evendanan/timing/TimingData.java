package net.evendanan.timing;

public class TimingData {
    public final int totalTasks;
    public final int doneTasks;
    public final long startTime;
    public final long totalTime;
    public final long estimatedTimeLeft;
    public final float ratioOfDone;

    public TimingData(int totalTasks, int doneTasks, long startTime, long totalTime, long estimatedTimeLeft, float ratioOfDone) {
        this.totalTasks = totalTasks;
        this.doneTasks = doneTasks;
        this.startTime = startTime;
        this.totalTime = totalTime;
        this.estimatedTimeLeft = estimatedTimeLeft;
        this.ratioOfDone = ratioOfDone;
    }
}
