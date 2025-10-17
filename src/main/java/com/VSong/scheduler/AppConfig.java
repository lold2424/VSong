package com.VSong.scheduler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AppConfig {

    @Bean(name = "vtuberSyncExecutor")
    public ThreadPoolExecutor vtuberSyncExecutor() {
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    }
}
