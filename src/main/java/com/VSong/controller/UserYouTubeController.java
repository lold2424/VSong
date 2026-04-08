package com.VSong.controller;

import com.VSong.entity.User;
import com.VSong.entity.VtuberSongsEntity;
import com.VSong.repository.UserRepository;
import com.VSong.repository.VtuberSongsRepository;
import com.VSong.service.GeminiService;
import com.VSong.service.UserYouTubeService;
import com.google.api.services.youtube.model.Playlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/youtube")
public class UserYouTubeController {

    private static final Logger logger = LoggerFactory.getLogger(UserYouTubeController.class);
    private final UserYouTubeService userYouTubeService;
    private final GeminiService geminiService;
    private final UserRepository userRepository;
    private final VtuberSongsRepository vtuberSongsRepository;

    public UserYouTubeController(UserYouTubeService userYouTubeService, 
                                 GeminiService geminiService, 
                                 UserRepository userRepository, 
                                 VtuberSongsRepository vtuberSongsRepository) {
        this.userYouTubeService = userYouTubeService;
        this.geminiService = geminiService;
        this.userRepository = userRepository;
        this.vtuberSongsRepository = vtuberSongsRepository;
    }

    @GetMapping("/recommend")
    public ResponseEntity<?> getRecommendations(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        
        String email = oAuth2User.getAttribute("email");
        User user = userRepository.findByEmail(email).orElseThrow();
        
        try {
            if (user.getLastKeywords() == null || user.getLastKeywords().isEmpty()) {
                return ResponseEntity.ok(Map.of("keywords", List.of(), "songs", List.of()));
            }

            List<String> keywords = List.of(user.getLastKeywords().split(",\\s*"));
            String regex = String.join("|", keywords);
            List<VtuberSongsEntity> recommendedSongs = vtuberSongsRepository.findByKeywords(regex, 10);
            
            Map<String, Object> response = new HashMap<>();
            response.put("keywords", keywords);
            response.put("songs", recommendedSongs);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("추천 곡 로딩 중 오류 발생", e);
            return ResponseEntity.status(500).body("추천 정보를 가져오는 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/recommend/refresh")
    public ResponseEntity<?> refreshRecommendations(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        
        String email = oAuth2User.getAttribute("email");
        User user = userRepository.findByEmail(email).orElseThrow();

        if (user.getLastRecommendationTime() != null && 
            user.getLastRecommendationTime().isAfter(LocalDateTime.now().minusMinutes(5))) {
            return ResponseEntity.status(429).body("추천 분석은 5분에 한 번만 가능합니다. 잠시 후 다시 시도해주세요.");
        }
        
        try {
            logger.info("AI 취향 재분석 요청: {}", user.getEmail());
            List<String> titles = userYouTubeService.getUserPlaylistTitles(user);
            List<String> keywords = geminiService.analyzeUserTaste(titles, user.getEmail());
            
            user.setLastKeywords(String.join(", ", keywords));
            user.setLastRecommendationTime(LocalDateTime.now());
            userRepository.save(user);
            
            String regex = String.join("|", keywords);
            List<VtuberSongsEntity> recommendedSongs = vtuberSongsRepository.findByKeywords(regex, 10);
            
            Map<String, Object> response = new HashMap<>();
            response.put("keywords", keywords);
            response.put("songs", recommendedSongs);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("추천 갱신 중 오류 발생", e);
            return ResponseEntity.status(500).body("추천 정보를 갱신하는 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/playlists")
    public ResponseEntity<?> getUserPlaylists(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        
        String email = oAuth2User.getAttribute("email");
        User user = userRepository.findByEmail(email).orElseThrow();
        
        try {
            List<Playlist> playlists = userYouTubeService.getUserPlaylists(user);
            return ResponseEntity.ok(playlists);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("재생목록을 가져오는 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/playlists/add")
    public ResponseEntity<?> addSongToPlaylist(@AuthenticationPrincipal OAuth2User oAuth2User,
                                               @RequestBody Map<String, String> request) {
        if (oAuth2User == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        
        String email = oAuth2User.getAttribute("email");
        User user = userRepository.findByEmail(email).orElseThrow();
        
        String videoId = request.get("videoId");
        String playlistId = request.get("playlistId");
        
        try {
            userYouTubeService.addVideoToPlaylist(user, videoId, playlistId);
            return ResponseEntity.ok("성공적으로 추가되었습니다.");
        } catch (Exception e) {
            logger.error("재생목록 추가 중 오류 발생", e);
            return ResponseEntity.status(500).body("노래를 추가하는 중 오류가 발생했습니다.");
        }
    }
}
