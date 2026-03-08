package com.sakura.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor taskExecutor(){
        // 完全使用 JUC 的原生线程池，这样才能原汁原味地使用优先队列
        return new ThreadPoolExecutor(
                1,
                2,
                60,
                TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}