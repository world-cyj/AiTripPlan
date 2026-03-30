package com.aitrip.backend.service;

public interface UserService {

    /** 手机号登录（验证码模式，测试环境固定验证码 123456） */
    String login(String phone, String code);

    /** 退出登录（Token 加入黑名单） */
    void logout(String token);

    /** 查询用户信息 */
    String getUserInfo(Long userId);

    /** 是否 VIP */
    boolean isVip(Long userId);

    /** 获取/更新用户旅行偏好（供 Agent 调用） */
    String getUserPreference(Long userId);
    void updatePreference(Long userId, String city, String typeIds, String budget);
}
