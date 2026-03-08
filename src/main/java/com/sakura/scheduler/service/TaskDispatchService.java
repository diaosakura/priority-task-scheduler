package com.sakura.scheduler.service;

import com.sakura.scheduler.task.PriorityTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class TaskDispatchService {

    @Autowired
    private ThreadPoolExecutor taskExecutor;

    @Autowired
    private AiAnalysisService aiAnalysisService;

    // 模拟 Redis 的延迟补偿调度器 (用于处理被拒绝的任务)
    private final ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(1);

    public String submitTask(String taskName, long delayMillis) {

        // 1. 【新增逻辑：预警丢弃策略防 OOM】
        // 假设我们的队列最多只能容纳 2 个任务，超过了就有 OOM 风险
        if (taskExecutor.getQueue().size() >= 2) {
            System.out.println("====== [系统高负载预警] 队列已满！触发降级拒绝策略 ======");
            // 触发指数退避重试逻辑 (模拟存入 Redis 延迟队列)
            handleRejectionWithBackoff(taskName, 1); // 初始重试次数为 1
            return "REJECTED_BUT_RETRYING";
        }

        // 2. 生成任务唯一 ID (必须放在使用它之前)
        String taskId = "TASK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 3. 调用 AI 获取权重
        int priority = aiAnalysisService.predictPriority(taskName);

        // 4. 【新增亮点：针对预测的大型任务动态调整线程池参数】
        // 假设 priority 为 100 代表超级笨重的大型任务 (比如：生成年度报表)
        if (priority == 100) {
            int currentCoreSize = taskExecutor.getCorePoolSize();
            // 如果核心线程数还不够多，动态扩容，借调更多底层资源来抗压
            if (currentCoreSize < 4) {
                System.out.println("====== [动态扩容] 检测到重度任务，核心线程数从 " + currentCoreSize + " 提升至 " + (currentCoreSize + 1) + " ======");
                taskExecutor.setCorePoolSize(currentCoreSize + 1);
            }
        }

        // 5. 封装任务并投递
        PriorityTask task = new PriorityTask(taskId, priority, delayMillis);
        taskExecutor.execute(task);

        System.out.println("[调度中心] 接收新任务: " + taskName + " | 评估权重: " + priority + " | 分配ID: " + taskId);

        return taskId;
    }

    /**
     * 【核心高阶逻辑：指数退避重试】
     * 第一次被拒绝：等 2 秒再试
     * 第二次被拒绝：等 4 秒再试
     * 第三次被拒绝：等 8 秒再试 ...
     */
    private void handleRejectionWithBackoff(String taskName, int retryCount) {
        // 计算指数延迟时间：2 的 retryCount 次方 (2s, 4s, 8s...)
        long delaySeconds = (long) Math.pow(2, retryCount);

        System.out.println("[降级补偿] 任务 [" + taskName + "] 已放入延迟队列，将在 " + delaySeconds + " 秒后进行第 " + retryCount + " 次重试");

        // 模拟 Redis 延迟队列到期后重新投递
        retryScheduler.schedule(() -> {
            System.out.println("[延迟补偿触发] 正在重新尝试投递任务: " + taskName);
            // 重新调用 submitTask，如果还是满的，它会自己进入下一次退避
            submitTask(taskName, 0);
        }, delaySeconds, TimeUnit.SECONDS);
    }
}