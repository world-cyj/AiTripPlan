package com.aitrip.agent.mcp;

import com.aitrip.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP Tool 定义 — 将后端服务能力暴露给 Agent
 *
 * 规范：
 * - 所有方法返回 String（JSON）
 * - 正常：{"success": true, "data": {...}}
 * - 异常：{"error": true, "code": "...", "message": "...", "suggestion": "..."}
 * - description 必须包含：功能 + 参数说明 + 返回格式 + 适用场景
 */
@Slf4j
@Component
public class BackendMcpTools {

    private static final String BACKEND_URL = "http://localhost:8080";

    private final RestTemplate restTemplate;

    public BackendMcpTools(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ==================== 景区相关 ====================

    @Tool(description = """
            按城市搜索景区列表。
            参数：city（城市名，如"西安"），keyword（关键词，可为空字符串）。
            返回 JSON 数组，每项包含 id/name/city/score/description。
            适用场景：用户询问某城市有哪些景点，或规划行程前获取景区列表。
            """)
    public String searchAttractions(
            @ToolParam(description = "城市名称，如：西安、北京、上海") String city,
            @ToolParam(description = "搜索关键词，可为空字符串") String keyword) {
        try {
            String url = BACKEND_URL + "/api/attractions/search?city={city}\u0026keyword={keyword}";
            Object resp = restTemplate.getForObject(url, Object.class, city, keyword);
            return successJson(resp);
        } catch (Exception e) {
            log.error("searchAttractions 失败", e);
            return errorJson("MCP_CALL_FAILED", "搜索景区失败: " + e.getMessage(), "请检查后端服务是否正常");
        }
    }

    @Tool(description = """
            查询指定景区的详细信息。
            参数：attractionId（景区ID，整数）。
            返回 JSON：包含 id/name/city/description/score/address/openHours 等字段。
            适用场景：用户想了解某景区的详细介绍、开放时间、地址等信息。
            """)
    public String getAttractionDetail(
            @ToolParam(description = "景区ID，整数，如：1001") Long attractionId) {
        try {
            String url = BACKEND_URL + "/api/attractions/{id}";
            Object resp = restTemplate.getForObject(url, Object.class, attractionId);
            return successJson(resp);
        } catch (Exception e) {
            log.error("getAttractionDetail 失败", e);
            return errorJson("MCP_CALL_FAILED", "查询景区详情失败: " + e.getMessage(), "请确认景区ID是否正确");
        }
    }

    @Tool(description = """
            查询指定景区的门票库存余量。
            参数：attractionId（景区ID，整数）。
            返回 JSON：{attractionId, stock, available}，stock 为余票数，available 为是否有票。
            适用场景：规划行程前确认票务情况，或用户询问某景区是否还有票。
            """)
    public String queryTicketStock(
            @ToolParam(description = "景区ID，整数，如：1001") Long attractionId) {
        try {
            String url = BACKEND_URL + "/api/attractions/{id}/stock";
            Object resp = restTemplate.getForObject(url, Object.class, attractionId);
            return successJson(resp);
        } catch (Exception e) {
            log.error("queryTicketStock 失败", e);
            return errorJson("STOCK_EMPTY", "查询库存失败: " + e.getMessage(), "请确认景区ID是否正确");
        }
    }

    // ==================== 秒杀相关 ====================

    @Tool(description = """
            预校验用户的秒杀资格（不扣库存，仅检查）。
            参数：voucherId（优惠券ID），userId（用户ID）。
            返回 JSON：{eligible, stock, alreadyBought, reason}。
            适用场景：行程规划完成后，在正式抢票前判断用户是否有资格参与。
            """)
    public String preCheckSeckillEligibility(
            @ToolParam(description = "优惠券/活动ID") Long voucherId,
            @ToolParam(description = "用户ID") Long userId) {
        try {
            String url = BACKEND_URL + "/api/seckill/pre-check?voucherId={v}\u0026userId={u}";
            Object resp = restTemplate.getForObject(url, Object.class, voucherId, userId);
            return successJson(resp);
        } catch (Exception e) {
            log.error("preCheckSeckillEligibility 失败", e);
            return errorJson("MCP_CALL_FAILED", "资格预校验失败: " + e.getMessage(), null);
        }
    }

    @Tool(description = """
            执行秒杀抢票。【写操作，调用前须用户明确确认，不可自动触发】
            参数：voucherId（优惠券ID），userId（用户ID）。
            返回 JSON：成功时 {orderId, message}，失败时 {error, code, message, suggestion}。
            适用场景：用户明确说"帮我抢票"或"立即购买"时调用。
            """)
    public String seckillTicket(
            @ToolParam(description = "优惠券/活动ID") Long voucherId,
            @ToolParam(description = "用户ID") Long userId) {
        try {
            String url = BACKEND_URL + "/api/seckill/execute";
            Map\u003cString, Long\u003e body = new HashMap\u003c\u003e();
            body.put("voucherId", voucherId);
            body.put("userId", userId);
            Object resp = restTemplate.postForObject(url, body, Object.class);
            return successJson(resp);
        } catch (Exception e) {
            log.error("seckillTicket 失败", e);
            return errorJson("MCP_CALL_FAILED", "秒杀失败: " + e.getMessage(),
                    "库存可能已售完，可调用 subscribeTicketNotify 订阅到货提醒");
        }
    }

    @Tool(description = """
            订阅到票通知。当指定优惠券有退票时，系统提前2分钟推送通知。
            参数：voucherId（优惠券ID），userId（用户ID）。
            返回 JSON：{subscribed, message}。
            适用场景：票已售完，用户希望有退票时收到通知。
            """)
    public String subscribeTicketNotify(
            @ToolParam(description = "优惠券/活动ID") Long voucherId,
            @ToolParam(description = "用户ID") Long userId) {
        try {
            String url = BACKEND_URL + "/api/notify/subscribe?voucherId={v}\u0026userId={u}";
            Object resp = restTemplate.postForObject(url, null, Object.class, voucherId, userId);
            return successJson(resp);
        } catch (Exception e) {
            log.error("subscribeTicketNotify 失败", e);
            return errorJson("MCP_CALL_FAILED", "订阅通知失败: " + e.getMessage(), null);
        }
    }

    // ==================== 用户偏好 ====================

    @Tool(description = """
            获取用户的旅行偏好（城市偏好、景区类型、预算）。
            参数：userId（用户ID）。
            返回 JSON：{city, typeIds, budget}。
            适用场景：规划行程前了解用户偏好，使推荐更个性化。
            """)
    public String getUserPreference(
            @ToolParam(description = "用户ID") Long userId) {
        try {
            String url = BACKEND_URL + "/api/user/{id}/preference";
            Object resp = restTemplate.getForObject(url, Object.class, userId);
            return successJson(resp);
        } catch (Exception e) {
            return errorJson("MCP_CALL_FAILED", "获取用户偏好失败: " + e.getMessage(), null);
        }
    }

    @Tool(description = """
            更新用户的旅行偏好。对话中获取用户偏好后调用保存。
            参数：userId, city, typeIds（逗号分隔如1,2）, budget（如500元/天）。
            适用场景：用户表达偏好后自动保存供下次规划使用。
            """)
    public String updateUserPreference(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "偏好城市") String city,
            @ToolParam(description = "景区类型ID，1=历史 2=美食 3=自然") String typeIds,
            @ToolParam(description = "预算，如500元/天") String budget) {
        try {
            String url = BACKEND_URL + "/api/user/{id}/preference";
            Map<String, String> body = new HashMap<>();
            body.put("city", city); body.put("typeIds", typeIds); body.put("budget", budget);
            restTemplate.postForObject(url, body, Object.class, userId);
            return JsonUtil.toJson(Map.of("success", true, "message", "偏好已保存"));
        } catch (Exception e) {
            return errorJson("MCP_CALL_FAILED", "更新偏好失败: " + e.getMessage(), null);
        }
    }

    // ==================== 热榜 ====================

    @Tool(description = """
            获取全站景区热榜（按综合热度排序）。
            参数：topN（榜单数量，默认10）。
            返回 JSON 数组：[{rank, name, city, score, hotScore, uv}]。
            适用场景：用户询问最热门景点时调用。
            """)
    public String getHotAttractions(
            @ToolParam(description = "返回榜单数量，默认10") int topN) {
        try {
            String url = BACKEND_URL + "/api/rank/hot?topN={n}";
            Object resp = restTemplate.getForObject(url, Object.class, topN);
            return successJson(resp);
        } catch (Exception e) {
            return errorJson("MCP_CALL_FAILED", "获取热榜失败: " + e.getMessage(), null);
        }
    }

    @Tool(description = """
            获取指定城市景区热榜。
            参数：city（城市名），topN（数量）。
            适用场景：用户询问某城市最热门景区时调用。
            """)
    public String getCityHotAttractions(
            @ToolParam(description = "城市名称") String city,
            @ToolParam(description = "返回数量，默认5") int topN) {
        try {
            String url = BACKEND_URL + "/api/rank/city/{city}?topN={n}";
            Object resp = restTemplate.getForObject(url, Object.class, city, topN);
            return successJson(resp);
        } catch (Exception e) {
            return errorJson("MCP_CALL_FAILED", "获取城市热榜失败: " + e.getMessage(), null);
        }
    }

    @Tool(description = """
            查询用户订单列表。
            参数：userId, page（页码）, size（每页数量）。
            返回 JSON：{total, records:[{orderId, voucherId, statusDesc, createTime}]}。
            适用场景：用户询问我的订单时调用。
            """)
    public String getUserOrders(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "页码，从1开始") int page,
            @ToolParam(description = "每页数量") int size) {
        try {
            String url = BACKEND_URL + "/api/orders/user/{uid}?page={p}&size={s}";
            Object resp = restTemplate.getForObject(url, Object.class, userId, page, size);
            return successJson(resp);
        } catch (Exception e) {
            return errorJson("MCP_CALL_FAILED", "查询订单失败: " + e.getMessage(), null);
        }
    }

    // ==================== 私有辅助 ====================

    private String successJson(Object data) {
        Map\u003cString, Object\u003e res = new HashMap\u003c\u003e();
        res.put("success", true);
        res.put("data", data);
        return JsonUtil.toJson(res);
    }

    private String errorJson(String code, String message, String suggestion) {
        Map\u003cString, Object\u003e res = new HashMap\u003c\u003e();
        res.put("error", true);
        res.put("code", code);
        res.put("message", message);
        if (suggestion != null) res.put("suggestion", suggestion);
        return JsonUtil.toJson(res);
    }
}
