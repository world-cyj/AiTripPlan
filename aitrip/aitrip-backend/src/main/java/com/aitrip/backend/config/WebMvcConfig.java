package com.aitrip.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 全局跨域配置
 * 允许前端（file:// 或 localhost 任意端口）访问后端接口
 */
@Configuration
public class WebMvcConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 明确允许的来源（包含 file:// 的 null origin 和各种 localhost 端口）
        config.setAllowedOrigins(List.of(
                "http://localhost:5500",
                "http://localhost:3000",
                "http://localhost:8080",
                "http://localhost:8081",
                "http://127.0.0.1:5500",
                "http://127.0.0.1:3000"
        ));
        // 同时允许所有 origin pattern（兜底）
        config.addAllowedOriginPattern("*");

        // 允许所有请求头
        config.addAllowedHeader("*");

        // 允许所有方法
        config.addAllowedMethod("*");

        // 不携带 Cookie（避免 allowCredentials+wildcard 冲突）
        config.setAllowCredentials(false);

        // 预检缓存 1 小时
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
