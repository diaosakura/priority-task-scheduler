package com.sakura.scheduler.task;
import java.util.concurrent.atomic.AtomicReference;

public class PriorityTask implements Runnable, Comparable<PriorityTask>{
    private final String taskId;

    // 数值越小优先级越高
    private final int priority;

    // 延迟执行时间（时间戳）
    private final long executeTime;

    private final AtomicReference<TaskStatus> status =
            new AtomicReference<>(TaskStatus.WAITING);

    public PriorityTask(String taskId, int priority, long delayMillis) {
        this.taskId = taskId;
        this.priority = priority;
        this.executeTime = System.currentTimeMillis() + delayMillis;
    }

    @Override
    public void run() {
        if (!status.compareAndSet(TaskStatus.WAITING, TaskStatus.RUNNING)) {
            return;
        }

        try {
            System.out.println("Executing task: " + taskId +
                    " | Priority: " + priority +
                    " | Thread: " + Thread.currentThread().getName());

            Thread.sleep(1000); // 模拟任务执行

            status.set(TaskStatus.SUCCESS);
        } catch (Exception e) {
            status.set(TaskStatus.FAILED);
        }
    }

    @Override
    public int compareTo(PriorityTask other) {

        // 先按执行时间排序（延迟任务）
        int timeCompare = Long.compare(this.executeTime, other.executeTime);
        if (timeCompare != 0) {
            return timeCompare;
        }

        // 再按优先级排序
        return Integer.compare(this.priority, other.priority);
    }
    
    public long getExecuteTime() {
        return executeTime;
    }


}
