package com.aitrip.agent.react;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * ReAct Agent — 行程规划主入口
 * 模式：Reason + Act 循环，直到给出最终答案
 */
@Slf4j
@Service
public class TripPlanReActAgent {

    private static final String SYSTEM_PROMPT = """
            你是 AiTrip 的智能旅游规划师。你的核心能力：
            1. 理解用户的旅游意图（目的地、天数、偏好、预算）
            2. 自动搜索景区、查询余票、生成带时间轴的行程方案
            3. 在用户确认后，代用户完成票务预订
            
            行动规范（ReAct 模式）：
            - 每次 Tool Call 前说明"我要做什么"（Thought）
            - 观察 Tool 返回结果（Observation），再决定下一步
            - 票务秒杀前必须先问用户确认，不得自动执行
            - 若某景区无票，主动提示订阅功能
            - 最终行程方案使用 Markdown 格式，包含日期/时间/景区/票价/注意事项
            
            错误处理：
            - STOCK_EMPTY：告知用户票已售完，询问是否订阅到票提醒
            - RATE_LIMIT：等待1-2秒后重试
            - SYSTEM_ERROR：建议用户稍后重试
            """;

    private final ChatClient chatClient;

    public TripPlanReActAgent(ChatClient.Builder builder,
                               ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(toolCallbackProvider)
                .build();
    }

    /**
     * 流式返回（SSE 推送给前端）
     */
    public Flux<String> planTrip(String userId, String userMessage, String conversationId) {
        log.info("[ReAct] userId={} conversationId={}", userId, conversationId);
        return chatClient.prompt()
                .user(userMessage)
                .stream()
                .content();
    }

    /**
     * 同步调用（用于 A2A 场景）
     */
    public String planTripSync(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }
}
