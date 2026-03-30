package com.aitrip.agent.a2a;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * 景区推荐专职 Agent（A2A）
 * 被 ReAct 主 Agent 调用，专注景区推荐
 */
@Slf4j
@Component
public class AttractionRecommendAgent {

    private static final String SYSTEM_PROMPT = """
            你是景区推荐专家。根据目的地城市和用户偏好，
            使用 searchAttractions 工具搜索并推荐最适合的景区列表。
            推荐时考虑：评分、与用户偏好的匹配度、游览时长。
            返回格式：Markdown 列表，每项包含景区名、推荐理由、建议游览时长。
            """;

    private final ChatClient chatClient;

    public AttractionRecommendAgent(ChatClient.Builder builder,
                                     ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(toolCallbackProvider)
                .build();
    }

    /**
     * 推荐景区
     * @param city       目的地城市
     * @param preference 用户偏好（历史/自然/美食等）
     * @return Markdown 格式景区推荐列表
     */
    public String recommend(String city, String preference) {
        log.info("[A2A] AttractionRecommendAgent city={} preference={}", city, preference);
        return chatClient.prompt()
                .user("请为去%s、偏好%s的用户推荐景区".formatted(city, preference))
                .call()
                .content();
    }
}
