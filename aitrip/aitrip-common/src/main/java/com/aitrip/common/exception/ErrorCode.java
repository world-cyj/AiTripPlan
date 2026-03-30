package com.aitrip.common.exception;

import lombok.Getter;

/**
 * 统一错误码枚举
 */
@Getter
public enum ErrorCode {

    // 通用
    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统内部错误"),

    // 票务 / 秒杀
    STOCK_EMPTY(1001, "门票库存不足"),
    REPEAT_BUY(1002, "每人限购一张，请勿重复购买"),
    RATE_LIMIT(1003, "请求过于频繁，请稍后再试"),
    SECKILL_NOT_START(1004, "秒杀活动尚未开始"),
    SECKILL_ENDED(1005, "秒杀活动已结束"),
    VOUCHER_NOT_FOUND(1006, "优惠券不存在"),

    // 景区
    ATTRACTION_NOT_FOUND(2001, "景区不存在"),
    ATTRACTION_ID_INVALID(2002, "景区 ID 不合法"),

    // 订单
    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_STATUS_ERROR(3002, "订单状态异常"),

    // Agent / MCP
    MCP_CALL_FAILED(4001, "MCP 工具调用失败"),
    AGENT_TIMEOUT(4002, "Agent 规划超时");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
