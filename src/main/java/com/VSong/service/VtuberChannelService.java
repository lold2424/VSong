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
        if (gender == null || gender.equalsIgnoreCase("all")) {
            return vtuberRepository.findAllChannelIds();
        } else if (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("female")) {
            return vtuberRepository.findChannelIdsByGender(gender.toLowerCase());
        } else if (gender.equalsIgnoreCase("mixed")) {
            return vtuberRepository.findChannelIdsWithNullGender();
        } else {
            return vtuberRepository.findAllChannelIds();
        }
    }
}
