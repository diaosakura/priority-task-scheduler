package com.sakura.scheduler.service;

import org.springframework.stereotype.Service;

@Service
public class AiAnalysisService {

    // 【速成阶段暂不开启】等你准备好了 pom.xml 和 API key，再把这两行解开
    // @Autowired
    // private ChatClient chatClient;

    public int predictPriority(String taskDesc) {
        // 模拟 AI 逻辑返回权重
        // 配合你 PriorityTask 的逻辑：数值越小，优先级越高！
        if (taskDesc.contains("验证码")) {
            return 1; // 极高优先级
        }
        if (taskDesc.contains("报表")) {
            return 100; // 最低优先级，慢慢排队
        }

        return 50; // 普通任务
    }
}