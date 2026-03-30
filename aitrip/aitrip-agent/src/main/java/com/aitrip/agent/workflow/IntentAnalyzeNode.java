package com.aitrip.agent.workflow;

import com.aitrip.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 意图解析节点
 * 从用户自然语言输入中提取：目的地城市、天数、偏好
 */
@Slf4j
@Component
public class IntentAnalyzeNode {

    private static final String PROMPT = """
            从以下用户输入中提取旅游意图，以JSON格式返回，只返回JSON不要额外说明：
            {
              "city": "目的地城市（字符串）",
              "days": 天数（整数，默认2）,
              "preference": "偏好，从[历史遗迹,自然风光,主题公园,美食街区]中选择，多个用逗号分隔"
            }
            用户输入：{input}
            """;

    private final ChatClient chatClient;

    public IntentAnalyzeNode(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public TripPlanState execute(TripPlanState state) {
        log.info("[WorkFlow] 步骤1-意图解析 input={}", state.getUserInput());
        state.setCurrentStep("analyze_intent");
        try {
            String result = chatClient.prompt()
                    .user(PROMPT.replace("{input}", state.getUserInput()))
                    .call()
                    .content();

            // 提取 JSON（去掉可能的 markdown 代码块包装）
            String json = extractJson(result);
            Map<?, ?> parsed = JsonUtil.fromJson(json, Map.class);
            if (parsed != null) {
                state.setCity(str(parsed.get("city")));
                state.setDays(parsed.get("days") != null
                        ? Integer.parseInt(parsed.get("days").toString()) : 2);
                state.setPreference(str(parsed.get("preference")));
            }
            log.info("[WorkFlow] 意图解析完成 city={} days={} preference={}",
                    state.getCity(), state.getDays(), state.getPreference());
        } catch (Exception e) {
            log.error("[WorkFlow] 意图解析失败", e);
            // 降级：尝试从原始输入直接提取城市名
            state.setCity(state.getUserInput());
            state.setDays(2);
            state.setPreference("历史遗迹");
        }
        return state;
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end   = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }
}
