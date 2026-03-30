package com.aitrip.agent.workflow;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 行程规划 WorkFlow 状态
 * 在各处理节点间传递，累积结果
 */
@Data
@Builder
public class TripPlanState {

    /** 用户ID */
    private String userId;

    /** 原始用户输入 */
    private String userInput;

    // ---- 意图解析结果 ----
    /** 目的地城市 */
    private String city;

    /** 旅游天数 */
    private Integer days;

    /** 用户偏好（历史/自然/美食/主题公园） */
    private String preference;

    // ---- 景区搜索结果 ----
    /** 搜索到的景区列表（JSON字符串） */
    private String attractionsJson;

    /** 解析后的景区列表 */
    private List<Map<String, Object>> attractions;

    // ---- 票务查询结果 ----
    /** 各景区票务信息（JSON字符串） */
    private String ticketsJson;

    // ---- 最终输出 ----
    /** 最终行程方案（Markdown格式） */
    private String finalPlan;

    /** 当前步骤（用于日志/监控） */
    private String currentStep;

    /** 是否发生错误 */
    private boolean hasError;

    /** 错误信息 */
    private String errorMessage;
}
