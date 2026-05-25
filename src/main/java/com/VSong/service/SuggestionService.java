package com.VSong.service;

import com.VSong.entity.SuggestionEntity;
import com.VSong.repository.SuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;

    public SuggestionService(SuggestionRepository suggestionRepository) {
        this.suggestionRepository = suggestionRepository;
    }

    @Transactional
    public SuggestionEntity saveSuggestion(String content, String userEmail, String userName, String ipAddress) {
        if (content == null || content.trim().length() < 10) {
            throw new RuntimeException("건의사항 내용은 최소 10자 이상 입력해주세요.");
        }
        if (content.length() > 1000) {
            throw new RuntimeException("건의사항 내용은 최대 1000자까지 입력 가능합니다.");
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        if (userEmail != null && !userEmail.isEmpty()) {
            long count = suggestionRepository.countByUserEmailAndCreatedAtAfter(userEmail, oneHourAgo);
            if (count >= 5) {
                throw new RuntimeException("건의사항은 1시간에 최대 5건까지만 제출할 수 있습니다.");
            }
        }

        if (ipAddress != null && !ipAddress.isEmpty()) {
            long ipCount = suggestionRepository.countByIpAddressAndCreatedAtAfter(ipAddress, oneHourAgo);
            if (ipCount >= 5) {
                throw new RuntimeException("동일한 IP에서 1시간에 최대 5건까지만 제출할 수 있습니다.");
            }
        }

        SuggestionEntity suggestion = new SuggestionEntity();
        suggestion.setContent(content);
        suggestion.setUserEmail(userEmail);
        suggestion.setUserName(userName);
        suggestion.setIpAddress(ipAddress);

        suggestion.setType("GENERAL");
        
        return suggestionRepository.save(suggestion);
    }

    public List<SuggestionEntity> getAllSuggestions() {
        return suggestionRepository.findAllByOrderByCreatedAtDesc();
    }
}
