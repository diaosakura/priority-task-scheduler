package com.sakura.scheduler.controller;

import com.sakura.scheduler.service.TaskDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    @Autowired
    private TaskDispatchService taskDispatchService;

    /**
     * 提交任务接口
     * 测试 URL: POST http://localhost:8080/api/v1/tasks/submit?taskName=生成年度报表
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitTask(
            @RequestParam String taskName,
            @RequestParam(defaultValue = "0") long delayMillis) {

        // 调用 Service 提交任务
        String taskId = taskDispatchService.submitTask(taskName, delayMillis);

        // 组装企业级规范的 JSON 返回体
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "任务提交成功，正在排队执行");
        response.put("data", Map.of("taskId", taskId, "taskName", taskName));

        return ResponseEntity.ok(response);
    }
}
