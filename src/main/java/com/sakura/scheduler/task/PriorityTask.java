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

            Thread.sleep(10000); // 模拟任务执行

            status.set(TaskStatus.SUCCESS);
        } catch (Exception e) {
            status.set(TaskStatus.FAILED);
        }
    }

    @Override
    public int compareTo(PriorityTask other) {

        // 1. 首要条件：先按 AI 预估的优先级排序 (数值越小，优先级越高)
        int priorityCompare = Integer.compare(this
                .priority, other.priority);
        if (priorityCompare != 0
        ) {
            return
                    priorityCompare;
        }

        // 2. 次要条件：如果两个任务优先级完全一样，再按提交时间排序（先提交的先执行，防止饿死）
        return Long.compare(this
                .executeTime, other.executeTime);
    }
    
    public long getExecuteTime() {
        return executeTime;
    }


}
