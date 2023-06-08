package org.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"org.core", "org.web", "org.api", "org.oss", "org.plugin"})
@EnableAsync // 开启任务
@EnableScheduling // 开启定时任务
public class MusicBoxSpringBoot {
    public static void main(String[] args) {
        SpringApplication.run(MusicBoxSpringBoot.class, args);
    }
}