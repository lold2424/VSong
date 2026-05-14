package com.VSong.service;

import com.VSong.entity.SuggestionEntity;
import com.VSong.repository.SuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;

    public SuggestionService(SuggestionRepository suggestionRepository) {
        this.suggestionRepository = suggestionRepository;
    }

    @Transactional
    public SuggestionEntity saveSuggestion(String content, String userEmail, String userName) {
        SuggestionEntity suggestion = new SuggestionEntity();
        suggestion.setContent(content);
        suggestion.setUserEmail(userEmail);
        suggestion.setUserName(userName);
        return suggestionRepository.save(suggestion);
    }

    public List<SuggestionEntity> getAllSuggestions() {
        return suggestionRepository.findAllByOrderByCreatedAtDesc();
    }
}
