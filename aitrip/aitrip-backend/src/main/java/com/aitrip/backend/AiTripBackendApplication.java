package com.aitrip.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * AiTrip 后端服务启动类
 * 端口：8080
 */
@SpringBootApplication(scanBasePackages = {"com.aitrip.backend", "com.aitrip.common"})
@MapperScan("com.aitrip.backend.mapper")
@EnableKafka
@EnableScheduling
public class AiTripBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTripBackendApplication.class, args);
    }
}
