package com.aitrip.backend.service.impl;

import com.aitrip.backend.constant.RedisKeyConstants;
import com.aitrip.backend.entity.TbUser;
import com.aitrip.backend.mapper.UserMapper;
import com.aitrip.backend.service.UserService;
import com.aitrip.common.exception.BusinessException;
import com.aitrip.common.exception.ErrorCode;
import com.aitrip.common.util.JsonUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    /** 测试环境固定验证码 */
    private static final String TEST_CODE = "123456";
    /** Token 有效期 2 小时 */
    private static final long TOKEN_TTL_HOURS = 2;

    @Override
    public String login(String phone, String code) {
        // 验证码校验（生产环境替换为短信验证）
        if (!TEST_CODE.equals(code)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码错误");
        }

        // 查询或创建用户
        TbUser user = userMapper.selectOne(
                new QueryWrapper<TbUser>().eq("phone", phone));
        if (user == null) {
            user = new TbUser();
            user.setPhone(phone);
            user.setNickName("旅行者" + phone.substring(7));
            user.setLevel(1);
            userMapper.insert(user);
            log.info("[User] 新用户注册: phone={}", phone);
        }

        // 生成 Token（UUID）存入 Redis
        String token = UUID.randomUUID().toString().replace("-", "");
        String sessionKey = RedisKeyConstants.SESSION_USER + token;
        Map<String, String> sessionMap = new HashMap<>();
        sessionMap.put("userId", String.valueOf(user.getId()));
        sessionMap.put("phone", phone);
        sessionMap.put("nickName", user.getNickName());
        sessionMap.put("level", String.valueOf(user.getLevel()));
        redisTemplate.opsForHash().putAll(sessionKey, sessionMap);
        redisTemplate.expire(sessionKey, TOKEN_TTL_HOURS, TimeUnit.HOURS);

        return JsonUtil.toJson(Map.of(
                "token", token,
                "userId", user.getId(),
                "nickName", user.getNickName(),
                "level", user.getLevel()
        ));
    }

    @Override
    public void logout(String token) {
        String sessionKey = RedisKeyConstants.SESSION_USER + token;
        redisTemplate.delete(sessionKey);
        // 加入黑名单（防止 Token 被继续使用）
        redisTemplate.opsForValue().set(
                RedisKeyConstants.TOKEN_BLACKLIST + token, "1", TOKEN_TTL_HOURS, TimeUnit.HOURS);
        log.info("[User] 用户退出登录: token={}", token.substring(0, 8) + "...");
    }

    @Override
    public String getUserInfo(Long userId) {
        TbUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在");
        }
        return JsonUtil.toJson(Map.of(
                "userId", user.getId(),
                "nickName", user.getNickName(),
                "phone", maskPhone(user.getPhone()),
                "level", user.getLevel()
        ));
    }

    @Override
    public boolean isVip(Long userId) {
        TbUser user = userMapper.selectById(userId);
        return user != null && user.getLevel() != null && user.getLevel() >= 2;
    }

    @Override
    public String getUserPreference(Long userId) {
        String key = RedisKeyConstants.USER_PREFERENCE + userId;
        Map<Object, Object> pref = redisTemplate.opsForHash().entries(key);
        if (pref.isEmpty()) {
            return JsonUtil.toJson(Map.of(
                    "city", "", "typeIds", "", "budget", "不限",
                    "tip", "用户尚未设置偏好，建议询问用户后调用 updatePreference"));
        }
        return JsonUtil.toJson(pref);
    }

    @Override
    public void updatePreference(Long userId, String city, String typeIds, String budget) {
        String key = RedisKeyConstants.USER_PREFERENCE + userId;
        Map<String, String> pref = new HashMap<>();
        if (city != null)    pref.put("city", city);
        if (typeIds != null) pref.put("typeIds", typeIds);
        if (budget != null)  pref.put("budget", budget);
        if (!pref.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, pref);
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
        }
        log.info("[User] 更新用户偏好: userId={}, pref={}", userId, pref);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
