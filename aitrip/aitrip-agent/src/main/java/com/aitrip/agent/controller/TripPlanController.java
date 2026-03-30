package com.aitrip.agent.controller;

import com.aitrip.agent.react.TripPlanReActAgent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 行程规划接口
 * 前端通过 EventSource / SSE 接收流式响应
 */
@RestController
@RequestMapping("/api/agent")
public class TripPlanController {

    private final TripPlanReActAgent agent;

    public TripPlanController(TripPlanReActAgent agent) {
        this.agent = agent;
    }

    /**
     * 流式行程规划
     * GET /api/agent/plan?message=帮我规划西安2日游&userId=1001&conversationId=xxx
     */
    @GetMapping(value = "/plan", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> plan(
            @RequestParam String message,
            @RequestParam(defaultValue = "anonymous") String userId,
            @RequestParam(defaultValue = "default") String conversationId) {
        return agent.planTrip(userId, message, conversationId);
    }

    /**
     * 同步行程规划（调试用）
     * POST /api/agent/plan/sync
     */
    @PostMapping("/plan/sync")
    public String planSync(@RequestBody String message) {
        return agent.planTripSync(message);
    }
}
