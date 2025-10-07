package com.VSong.service;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ApiChannelIdService {

    private final UploadVtuberService uploadVtuberService;

    public ApiChannelIdService(UploadVtuberService uploadVtuberService) {
        this.uploadVtuberService = uploadVtuberService;
    }

    /**
     * YouTube API 검색을 통해 모든 Vtuber의 채널 ID 목록을 반환합니다.
     * @return 채널 ID 목록
     */
    public List<String> getApiChannelIds() {
        return uploadVtuberService.fetchAllChannelIdsFromApi();
    }
}