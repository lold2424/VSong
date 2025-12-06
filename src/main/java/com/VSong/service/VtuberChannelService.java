package com.VSong.service;

import com.VSong.repository.VtuberRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VtuberChannelService {

    private final VtuberRepository vtuberRepository;

    public VtuberChannelService(VtuberRepository vtuberRepository) {
        this.vtuberRepository = vtuberRepository;
    }

    @Cacheable(value = "channelIds", key = "#gender")
    public List<String> getChannelIdsByGender(String gender) {
        if (gender == null || "all".equalsIgnoreCase(gender)) {
            return vtuberRepository.findAllChannelIds();
        } else if ("male".equalsIgnoreCase(gender) || "female".equalsIgnoreCase(gender)) {
            return vtuberRepository.findChannelIdsByGender(gender.toLowerCase());
        } else if ("mixed".equalsIgnoreCase(gender)) {
            return vtuberRepository.findChannelIdsWithNullGender();
        } else {
            return vtuberRepository.findAllChannelIds();
        }
    }
}
