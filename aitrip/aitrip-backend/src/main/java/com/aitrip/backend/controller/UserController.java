package com.aitrip.backend.controller;

import com.aitrip.backend.service.UserService;
import com.aitrip.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户接口
 * POST /api/user/login   — 手机号+验证码登录
 * POST /api/user/logout  — 退出登录
 * GET  /api/user/{id}    — 获取用户信息
 * GET  /api/user/{id}/preference — 获取用户旅行偏好
 * POST /api/user/{id}/preference — 更新用户旅行偏好
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 登录（测试环境验证码固定为 123456） */
    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> req) {
        String phone = req.get("phone");
        String code  = req.getOrDefault("code", "123456");
        return Result.ok(userService.login(phone, code));
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null) userService.logout(token);
        return Result.ok();
    }

    /** 用户信息 */
    @GetMapping("/{id}")
    public Result<String> getUserInfo(@PathVariable Long id) {
        return Result.ok(userService.getUserInfo(id));
    }

    /** 获取旅行偏好 */
    @GetMapping("/{id}/preference")
    public Result<String> getPreference(@PathVariable Long id) {
        return Result.ok(userService.getUserPreference(id));
    }

    /** 更新旅行偏好 */
    @PostMapping("/{id}/preference")
    public Result<Void> updatePreference(@PathVariable Long id,
                                         @RequestBody Map<String, String> req) {
        userService.updatePreference(id,
                req.get("city"),
                req.get("typeIds"),
                req.get("budget"));
        return Result.ok();
    }
}
