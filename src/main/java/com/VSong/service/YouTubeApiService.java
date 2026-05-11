package com.VSong.service;

import com.VSong.entity.ApiQuotaLog;
import com.VSong.repository.ApiQuotaLogRepository;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTubeRequest;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class YouTubeApiService {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeApiService.class);

    private final List<String> apiKeys;
    private final List<AtomicInteger> apiKeyUsage;
    private final List<Boolean> keyAvailable;
    private final ApiQuotaLogRepository apiQuotaLogRepository;
    private int currentKeyIndex = 0;

    private final RateLimiter rateLimiter = RateLimiter.create(5.0);

    public YouTubeApiService(@Value("${youtube.api.keys}") List<String> apiKeys,
                             ApiQuotaLogRepository apiQuotaLogRepository) {
        this.apiKeys = new ArrayList<>(apiKeys);
        this.apiKeyUsage = new ArrayList<>();
        this.keyAvailable = new ArrayList<>();
        this.apiQuotaLogRepository = apiQuotaLogRepository;
        for (String ignored : apiKeys) {
            this.apiKeyUsage.add(new AtomicInteger(0));
            this.keyAvailable.add(true);
        }
    }

    public String getCurrentApiKey() {
        return apiKeys.get(currentKeyIndex);
    }

    public void incrementApiUsage(int cost) {
        apiKeyUsage.get(currentKeyIndex).addAndGet(cost);
        if (apiKeyUsage.get(currentKeyIndex).get() >= 9500) {
            switchApiKey();
        }
    }

    private void switchApiKey() {
        keyAvailable.set(currentKeyIndex, false);
        int initialKeyIndex = currentKeyIndex;
        do {
            currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
            if (keyAvailable.get(currentKeyIndex)) {
                if (logger.isInfoEnabled()) {
                    logger.info("API Key switched to index {}: {}", currentKeyIndex, getCurrentApiKey().substring(0, 10) + "...");
                }
                return;
            }
        } while (currentKeyIndex != initialKeyIndex);
        throw new RuntimeException("All API keys have been exhausted.");
    }

    public void resetDailyUsage() {
        for (int i = 0; i < apiKeyUsage.size(); i++) {
            apiKeyUsage.get(i).set(0);
            keyAvailable.set(i, true);
        }
        currentKeyIndex = 0;
        logger.info("Daily API usage has been reset.");
    }

    public List<Map<String, Object>> getApiKeysStatus() {
        List<Map<String, Object>> statusList = new ArrayList<>();
        for (int i = 0; i < apiKeys.size(); i++) {
            Map<String, Object> status = new java.util.HashMap<>();
            String key = apiKeys.get(i);
            status.put("index", i);
            status.put("keyPrefix", key.substring(0, Math.min(key.length(), 10)) + "...");
            status.put("usage", apiKeyUsage.get(i).get());
            status.put("isAvailable", keyAvailable.get(i));
            status.put("isCurrent", i == currentKeyIndex);
            statusList.add(status);
        }
        return statusList;
    }

    public <T> T executeRequest(YouTubeRequest<T> request) throws IOException {
        return executeRequest(request, null);
    }

    @SuppressWarnings("PMD.LooseCoupling")
    public <T> T executeRequest(YouTubeRequest<T> request, String context) throws IOException {
        rateLimiter.acquire();
        int attempts = 0;

        String className = request.getClass().getSimpleName();
        int cost = className.contains("Search") ? 100 : 1;
        String apiMethod = request.getClass().getEnclosingClass().getSimpleName() + "." + 
                          className.substring(0, 1).toLowerCase() + className.substring(1);

        while (attempts < apiKeys.size()) {
            try {
                String currentKey = getCurrentApiKey();
                request.setKey(currentKey);
                incrementApiUsage(cost);

                try {
                    String keyPrefix = currentKey.substring(0, Math.min(currentKey.length(), 10)) + "...";
                    apiQuotaLogRepository.save(new ApiQuotaLog(keyPrefix, apiMethod, cost, context));
                } catch (Exception e) {
                    logger.error("Failed to save ApiQuotaLog", e);
                }

                return request.execute();
            } catch (GoogleJsonResponseException e) {
                String reason = (e.getDetails() != null && !e.getDetails().getErrors().isEmpty()) 
                        ? e.getDetails().getErrors().get(0).getReason() : "";

                if ("quotaExceeded".equals(reason)) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("API quota exceeded for key index {}. Switching to the next key and retrying.", currentKeyIndex);
                    }
                    switchApiKey();
                    attempts++;
                } else if (e.getStatusCode() == 403 || "accessNotConfigured".equals(reason) || "SERVICE_DISABLED".equals(reason)) {
                    if (logger.isErrorEnabled()) {
                        logger.error("API key at index {} is disabled or has no permission (Reason: {}). Switching to the next key.", currentKeyIndex, reason);
                    }
                    switchApiKey();
                    attempts++;
                } else {
                    throw e;
                }
            } catch (Exception e) {
                throw new IOException("An unexpected error occurred during API execution", e);
            }
        }
        throw new IOException("All API keys have exhausted their quotas. Cannot proceed.");
    }
}
