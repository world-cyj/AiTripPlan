package com.aitrip.agent.a2a;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 行程时间轴专职 Agent（A2A）
 * 根据景区列表和票务信息，生成详细的时间轴行程方案
 */
@Slf4j
@Component
public class TripScheduleAgent {

    private static final String SYSTEM_PROMPT = """
            你是行程规划专家，擅长生成合理的旅游时间轴方案。
            根据目的地、天数、景区列表和票务信息，生成详细行程。
            
            输出规范：
            - 使用 Markdown 格式
            - 按天分组（## Day 1、## Day 2...）
            - 每个景区标注：时间/地点/建议游览时长/门票价格/是否需要提前预订
            - 景点之间加入合理的交通方式和时间
            - 最后附上"注意事项"和"费用预估"
            """;

    private final ChatClient chatClient;

    public TripScheduleAgent(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    /**
     * 生成行程方案
     */
    public String generatePlan(String city, Integer days, String preference,
                                String attractionsJson, String ticketsJson) {
        log.info("[A2A] TripScheduleAgent city={} days={}", city, days);
        String prompt = """
                请为以下旅行生成详细时间轴行程方案：
                - 目的地：%s
                - 天数：%d天
                - 偏好：%s
                - 可选景区：%s
                - 票务信息：%s
                """.formatted(city, days, preference, attractionsJson, ticketsJson);
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
