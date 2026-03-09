package com.sakura.scheduler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
public class AiAnalysisService {

    private static final String API_KEY = "*";
    private static final String DEEPSEEK_URL = "https://api.deepseek.com/chat/completions";

    //  进化 1：单例 HttpClient，复用连接池
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .executor(Executors.newFixedThreadPool(10)) // 独立的线程池，不占用公共资源
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 进化 2：异步预测（sendAsync），绝不卡死调度主流程
     */
    public CompletableFuture<Integer> predictPriorityAsync(String taskDesc) {
        try {
            //  进化 3：使用 Map 生成 JSON，拒绝手动拼字符串（防止特殊字符导致解析失败）
            Map<String, Object> body = Map.of(
                    "model", "deepseek-chat",
                    "messages", List.of(Map.of("role", "user", "content", buildPrompt(taskDesc)))
            );
            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DEEPSEEK_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // 异步发送请求并链式处理结果
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(this::handleResponse)
                    .exceptionally(ex -> {
                        System.err.println("[AI 降级] 请求异常: " + ex.getMessage());
                        return 50; // 发生异常时返回默认优先级
                    });

        } catch (Exception e) {
            return CompletableFuture.completedFuture(50);
        }
    }

    private String buildPrompt(String desc) {
        return "你是一个后端资源调度专家。请根据任务描述，返回1-100的优先级分数(数字越小优先级越高)。仅返回纯数字。任务：" + desc;
    }

    private Integer handleResponse(HttpResponse<String> response) {
        try {
            if (response.statusCode() != 200) return 50;

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            //  进化 4：正则提取数字，增强健壮性
            return Integer.parseInt(content.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 50;
        }
    }
}