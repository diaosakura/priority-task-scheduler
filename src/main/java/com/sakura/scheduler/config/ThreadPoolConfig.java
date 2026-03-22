package com.sakura.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor taskExecutor(){
        return new ThreadPoolExecutor(
                1,
                2,
                60,
                TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(),
                // 【核心修改：真实落地的自定义拒绝策略】
                (r, executor) -> {
                    System.err.println("====== [底层拒绝策略触发] 线程池与队列已被彻底撑爆！ ======");
                    // 自定义逻辑：不抛出系统默认异常，而是抛出自定义的受检异常，
                    // 让上层业务代码去捕获它，并转入 Redis 延迟队列进行退避重试！
                    throw new RejectedExecutionException("CUSTOM_REJECT: 队列已满");
                }
        );
    }

}