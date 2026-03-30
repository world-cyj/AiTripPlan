package com.aitrip.agent.a2a;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * 票务查询专职 Agent（A2A）
 * 被 WorkFlow 或主 Agent 调用，专注票务查询
 */
@Slf4j
@Component
public class TicketQueryAgent {

    private static final String SYSTEM_PROMPT = """
            你是票务查询专家。根据景区列表查询每个景区的门票库存和价格。
            使用 queryTicketStock 工具查询每个景区的库存。
            返回格式：每个景区一行，包含景区名、余票数量、价格、是否可购买。
            如果某景区无票，注明"售完"。
            """;

    private final ChatClient chatClient;

    public TicketQueryAgent(ChatClient.Builder builder,
                             ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(toolCallbackProvider)
                .build();
    }

    /**
     * 查询景区列表的票务信息
     * @param city         目的地城市
     * @param attractionsJson 景区列表（JSON字符串）
     * @return 票务信息汇总
     */
    public String queryTickets(String city, String attractionsJson) {
        log.info("[A2A] TicketQueryAgent city={}", city);
        String prompt = "请查询以下%s景区的票务信息：\n%s".formatted(city, attractionsJson);
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
