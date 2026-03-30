package com.aitrip.agent.workflow;

import com.aitrip.agent.a2a.AttractionRecommendAgent;
import com.aitrip.agent.a2a.TicketQueryAgent;
import com.aitrip.agent.a2a.TripScheduleAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 行程规划 WorkFlow 服务
 * 串行执行确定性步骤：意图解析 → 景区推荐 → 票务查询 → 行程生成
 * 等价于 SpringAI Alibaba 的 StateGraph，但兼容标准 SpringAI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripWorkFlowService {

    private final IntentAnalyzeNode        intentAnalyzeNode;
    private final AttractionRecommendAgent attractionRecommendAgent;
    private final TicketQueryAgent         ticketQueryAgent;
    private final TripScheduleAgent        tripScheduleAgent;

    /**
     * 执行完整行程规划 WorkFlow
     *
     * @param userId    用户ID
     * @param userInput 用户输入
     * @return 最终行程方案（Markdown）
     */
    public String execute(String userId, String userInput) {
        log.info("[WorkFlow] 开始执行 userId={} input={}", userId, userInput);

        TripPlanState state = TripPlanState.builder()
                .userId(userId)
                .userInput(userInput)
                .build();

        // Step 1: 意图解析
        state = intentAnalyzeNode.execute(state);
        if (state.isHasError()) return errorPlan(state);

        // Step 2: 景区推荐（A2A）
        log.info("[WorkFlow] 步骤2-景区推荐 city={} preference={}",
                state.getCity(), state.getPreference());
        state.setCurrentStep("search_attractions");
        String attractions = attractionRecommendAgent.recommend(
                state.getCity(), state.getPreference());
        state.setAttractionsJson(attractions);

        // Step 3: 票务查询（A2A）
        log.info("[WorkFlow] 步骤3-票务查询");
        state.setCurrentStep("check_tickets");
        String tickets = ticketQueryAgent.queryTickets(
                state.getCity(), state.getAttractionsJson());
        state.setTicketsJson(tickets);

        // Step 4: 生成最终行程（A2A）
        log.info("[WorkFlow] 步骤4-生成行程方案");
        state.setCurrentStep("generate_plan");
        String plan = tripScheduleAgent.generatePlan(
                state.getCity(), state.getDays(), state.getPreference(),
                state.getAttractionsJson(), state.getTicketsJson());
        state.setFinalPlan(plan);

        log.info("[WorkFlow] 执行完成 userId={}", userId);
        return plan;
    }

    private String errorPlan(TripPlanState state) {
        return "**行程规划暂时失败**\n\n原因：" + state.getErrorMessage()
                + "\n\n请稍后重试，或直接告诉我您想去哪里。";
    }
}
