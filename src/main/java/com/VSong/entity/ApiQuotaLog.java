package com.VSong.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_quota_logs")
public class ApiQuotaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime requestTime;

    @Column(nullable = false)
    private String apiKeyPrefix;

    @Column(nullable = false)
    private String apiMethod;

    @Column(nullable = false)
    private Integer cost;

    @Column(columnDefinition = "TEXT")
    private String context;

    public ApiQuotaLog() {
    }

    public ApiQuotaLog(String apiKeyPrefix, String apiMethod, Integer cost, String context) {
        this.requestTime = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        this.apiKeyPrefix = apiKeyPrefix;
        this.apiMethod = apiMethod;
        this.cost = cost;
        this.context = context;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public String getApiKeyPrefix() {
        return apiKeyPrefix;
    }

    public void setApiKeyPrefix(String apiKeyPrefix) {
        this.apiKeyPrefix = apiKeyPrefix;
    }

    public String getApiMethod() {
        return apiMethod;
    }

    public void setApiMethod(String apiMethod) {
        this.apiMethod = apiMethod;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
