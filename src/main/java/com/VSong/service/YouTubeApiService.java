package com.VSong.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class YouTubeApiService {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeApiService.class);

    private final List<String> apiKeys;
    private final List<AtomicInteger> apiKeyUsage;
    private final List<Boolean> keyAvailable;
    private int currentKeyIndex = 0;

    private final RateLimiter rateLimiter = RateLimiter.create(5.0); // 5 requests per second

    public YouTubeApiService(@Value("${youtube.api.keys}") List<String> apiKeys) {
        this.apiKeys = new ArrayList<>(apiKeys);
        this.apiKeyUsage = new ArrayList<>();
        this.keyAvailable = new ArrayList<>();
        for (int i = 0; i < apiKeys.size(); i++) {
            this.apiKeyUsage.add(new AtomicInteger(0));
            this.keyAvailable.add(true);
        }
    }

    public String getCurrentApiKey() {
        return apiKeys.get(currentKeyIndex);
    }

    public void incrementApiUsage() {
        apiKeyUsage.get(currentKeyIndex).incrementAndGet();
        if (apiKeyUsage.get(currentKeyIndex).get() >= 9000) { // Threshold before hitting the limit
            switchApiKey();
        }
    }

    private void switchApiKey() {
        keyAvailable.set(currentKeyIndex, false);
        int initialKeyIndex = currentKeyIndex;
        do {
            currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
            if (keyAvailable.get(currentKeyIndex)) {
                logger.info("API Key switched to: {}", getCurrentApiKey());
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

    public <T> T executeRequest(Callable<T> apiCall) throws IOException {
        rateLimiter.acquire();
        int attempts = 0;
        while (attempts < apiKeys.size()) {
            try {
                incrementApiUsage();
                return apiCall.call();
            } catch (GoogleJsonResponseException e) {
                if (e.getDetails() != null && "quotaExceeded".equals(e.getDetails().getErrors().get(0).getReason())) {
                    logger.warn("API quota exceeded for key. Switching to the next key and retrying.");
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
