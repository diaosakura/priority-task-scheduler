package com.sakura.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PriorityTaskSchedulerApplication {

    public static void main(String[] args) {
        // 这行代码就是唤醒整个 Spring Boot 和 Tomcat 服务器的魔法
        SpringApplication.run(PriorityTaskSchedulerApplication.class, args);
        System.out.println("====== AI 任务调度系统启动成功！Tomcat 正在监听 8080 端口 ======");
    }

}