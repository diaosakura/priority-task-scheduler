package com.sakura.scheduler.service;

import com.sakura.scheduler.task.PriorityTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class TaskDispatchService {

    @Autowired
    private ThreadPoolExecutor taskExecutor;

    @Autowired
    private AiAnalysisService aiAnalysisService;

    // 模拟 Redis 的延迟补偿调度器
    private final ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(1);

    public String submitTask(String taskName, long delayMillis) {

        // 1. 【预警丢弃策略】：在进入线程池前先拦一道，防止无界队列导致 OOM
        if (taskExecutor.getQueue().size() >= 2) {
            System.out.println("====== [系统高负载预警] 拦截到 OOM 风险！ ======");
            handleRejectionWithBackoff(taskName, 1);
            return "REJECTED_BUT_RETRYING";
        }

        // 2. 生成任务唯一 ID（第一步就生成，为了极速响应前端）
        String taskId = "TASK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 3. 异步非阻塞调用 AI
        // 主线程不等 AI 返回，直接调用 Async 方法，把后续动作写在 thenAccept 回调里
        aiAnalysisService.predictPriorityAsync(taskName).thenAccept(priority -> {

            // 在 AI 评估完成后，由专门的 I/O 线程池来执行的
            if (priority == 100) {
                int currentCoreSize = taskExecutor.getCorePoolSize();
                if (currentCoreSize < 4) {
                    System.out.println("====== [动态扩容] 检测到重度任务，核心线程数提升至 " + (currentCoreSize + 1) + " ======");
                    taskExecutor.setCorePoolSize(currentCoreSize + 1);
                }
            }

            PriorityTask task = new PriorityTask(taskId, priority, delayMillis);

            try {
                // 尝试把任务丢进线程池
                taskExecutor.execute(task);
                System.out.println("[调度中心] 接收新任务: " + taskName + " | 评估权重: " + priority + " | 分配ID: " + taskId);
            } catch (RejectedExecutionException e) {
                // 4. 【捕获自定义拒绝策略的异常，触发延迟重试】
                System.out.println("[底层拦截] 任务投递失败，转入退避重试流程...");
                handleRejectionWithBackoff(taskName, 1);
            }
        });

        // 5. 主线程不等 AI 处理完，瞬间给前端/调用方返回 TaskID，做到接口毫秒级响应！
        return taskId;
    }

    private void handleRejectionWithBackoff(String taskName, int retryCount) {
        long delaySeconds = (long) Math.pow(2, retryCount);
        System.out.println("[降级补偿] 任务 [" + taskName + "] 已放入延迟队列，将在 " + delaySeconds + " 秒后进行第 " + retryCount + " 次重试");

        retryScheduler.schedule(() -> {
            System.out.println("[延迟补偿触发] 正在重新尝试投递任务: " + taskName);
            submitTask(taskName, 0); // 重新投递
        }, delaySeconds, TimeUnit.SECONDS);
    }
}