package com.sherwinzeng.cardiology.cardiologyauth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.sherwinzeng.cardiology.cardiologyauth.repository")
public class CardiologyAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardiologyAuthApplication.class, args);
    }
}
