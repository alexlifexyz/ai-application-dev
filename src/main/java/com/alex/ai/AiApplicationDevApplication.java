package com.alex.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Application Dev - Spring Boot 主启动类
 * 
 * @author Alex
 * @since 2025-12-31
 */
@SpringBootApplication
public class AiApplicationDevApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplicationDevApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("AI Application Dev 启动成功！");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("========================================\n");
    }
}
