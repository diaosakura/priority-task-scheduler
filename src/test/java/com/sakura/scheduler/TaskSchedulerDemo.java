package com.sakura.scheduler;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskSchedulerDemo {
    //1.任务状态常量
    private static final int PENDING =0;
    private static final int RUNNING =1;
    private static final int COMPLETED =2;

    //2.模拟任务类，实现 Comparable 接口用于 PriorityBlockingQueue 排序
    static class AiTask implements Comparable <AiTask>{
        String taskName;
        int priority;
        AtomicInteger status = new AtomicInteger(PENDING);
        public AiTask(String name, int priority)
        {
            this.taskName = name;
            this.priority = priority;
        }
        // 核心：优先级比较逻辑（大顶堆）
        @Override
        public int compareTo(AiTask other) {
            return Integer.compare(other.priority, this.priority);
        }

        // 使用 CAS 切换状态的方法
        public boolean tryStart() {
            // 如果当前是 PENDING(0)，则修改为 RUNNING(1)，否则失败
            return status.compareAndSet(PENDING, RUNNING);
        }
    }

    public static void main(String[] args)
    {
        // 3. 定义优先阻塞队列 (权重调度的核心)
        PriorityBlockingQueue<Runnable> queue =
                new
                        PriorityBlockingQueue<>();

        // 4. 自定义线程池 (JUC 核心)
        ThreadPoolExecutor executor =
                new
                        ThreadPoolExecutor(
                        2, 4, 60
                        , TimeUnit.SECONDS,
                        (BlockingQueue) queue,
                        new ThreadPoolExecutor.CallerRunsPolicy() // 简单的拒绝策略
                );

        // 5. 模拟 AI 分配权重并提交任务
        System.out.println(
                "--- AI 识别任务画像，分配动态优先级 ---"
        );
        submitTask(executor,
                "查询报表 (大型任务)", 10);  // 优先级低
        submitTask(executor,
                "短信验证码 (即时任务)", 99); // 优先级极高
        submitTask(executor,
                "数据清洗 (中型任务)", 50);  // 优先级中

        executor.shutdown();
    }

    private static void submitTask(ThreadPoolExecutor executor, String name, int aiPriority)
    {
        AiTask task =
                new
                        AiTask(name, aiPriority);

        executor.execute(() -> {
            // 使用 CAS 尝试锁定任务状态
            if
            (task.tryStart()) {
                System.out.println(
                        "[执行中] " + task.taskName + " | 权重: " + task.priority + " | 线程: "
                                + Thread.currentThread().getName());
                try
                {
                    Thread.sleep(
                            1000); // 模拟耗时
                }
                catch
                (InterruptedException e) {
                    e.printStackTrace();
                }
                task.status.set(COMPLETED);
                System.out.println(
                        "[已完成] "
                                + task.taskName);
            }
            else
            {
                System.out.println(
                        "[跳过] 任务已被其他线程抢占: "
                                + task.taskName);
            }
        });
    }
}
