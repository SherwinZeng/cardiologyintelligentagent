package com.sherwinzeng.cardiology.cardiologyrecord;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
@MapperScan("com.sherwinzeng.cardiology.cardiologyrecord.repository")
public class CardiologyRecordApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardiologyRecordApplication.class, args);
    }
}
