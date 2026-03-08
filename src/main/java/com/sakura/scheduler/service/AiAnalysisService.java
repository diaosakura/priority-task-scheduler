package com.sakura.scheduler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class AiAnalysisService {

    // ⚠️ 检查点 1：这里必须换成你自己真实的 API Key
    private static final String API_KEY = "sk-0928cd2adcf34a26a37b55b406e6b511";
    private static final String DEEPSEEK_URL = "https://api.deepseek.com/chat/completions";

    public int predictPriority(String taskDesc) {
        String prompt = "你是一个后端资源调度专家。请根据任务描述，返回1-100的优先级分数(数字越小优先级越高)。仅返回纯数字。任务：" + taskDesc;

        String requestBody = "{\n" + "  \"model\": " + "\"deepseek-chat\",\n" + "  \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]\n" +
                "}";

        try {
            System.out.println("[AI 调度节点] 正在呼叫 DeepSeek 分析任务: " + taskDesc);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10)) // 设置 10 秒超时
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DEEPSEEK_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // 发送请求
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // ⚠️ 检查点 2：把 DeepSeek 真正返回给我们的东西打印出来！
            System.out.println("[DeepSeek 原始返回] 状态码: " + response.statusCode() + " | 内容: " + response.body());

            // 如果状态码不是 200 (比如 401 没权限，402 没钱了)，直接降级
            if (response.statusCode() != 200) {
                System.err.println("[AI 拦截] DeepSeek 接口报错，触发降级！");
                return 50;
            }

            // 使用 Spring Boot 自带的 ObjectMapper 优雅解析 JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.body());

            // 提取 content 内容 (去除了之前粗暴的 indexOf 截取)
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();

            // 过滤掉 AI 可能废话带的标点符号，只保留数字
            int score = Integer.parseInt(content.replaceAll("[^0-9]", ""));
            System.out.println("[AI 分析完成] 评估得分为: " + score);

            return score;

        } catch (Exception e) {
            System.err.println("[AI 服务异常] 调用失败，触发降级策略返回 50！具体原因：" + e.getMessage());
            e.printStackTrace(); // 打印具体的错误堆栈
            return 50;
        }
    }
}