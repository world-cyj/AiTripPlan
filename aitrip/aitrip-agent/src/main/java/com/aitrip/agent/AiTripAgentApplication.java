package com.aitrip.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AiTrip Agent 服务启动类
 * 端口：8081
 * ToolCallbackProvider 配置见 AgentConfig
 */
@SpringBootApplication
public class AiTripAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTripAgentApplication.class, args);
    }
}
