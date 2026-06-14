package com.sherwinzeng.cardiology.cardiologysession;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients
@MapperScan("com.sherwinzeng.cardiology.cardiologysession.repository")
public class CardiologySessionApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardiologySessionApplication.class, args);
    }
}
