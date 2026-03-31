package com.VSong.service;

import com.VSong.entity.User;
import com.VSong.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        String accessToken = userRequest.getAccessToken().getTokenValue();
        
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        if (userRequest.getAccessToken().getExpiresAt() != null) {
            try {
                expiresAt = LocalDateTime.ofInstant(userRequest.getAccessToken().getExpiresAt(), java.time.ZoneId.systemDefault());
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(CustomOAuth2UserService.class).warn("만료 시간 변환 실패: {}", e.getMessage());
            }
        }

        String refreshToken = null;
        if (userRequest.getAdditionalParameters() != null && userRequest.getAdditionalParameters().containsKey("refresh_token")) {
            refreshToken = (String) userRequest.getAdditionalParameters().get("refresh_token");
        }

        final String finalRefreshToken = refreshToken;
        final LocalDateTime finalExpiresAt = expiresAt;

        try {
            User user = userRepository.findByEmail(email)
                    .map(entity -> {
                        entity.setLastLoginAt(LocalDateTime.now());
                        entity.setAccessToken(accessToken);
                        entity.setAccessTokenExpiresAt(finalExpiresAt);
                        if (finalRefreshToken != null) {
                            entity.setRefreshToken(finalRefreshToken);
                            entity.setRefreshTokenCreatedAt(LocalDateTime.now());
                        }
                        return entity;
                    })
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setRole(com.VSong.entity.Role.USER);
                        newUser.setAccessToken(accessToken);
                        newUser.setAccessTokenExpiresAt(finalExpiresAt);
                        if (finalRefreshToken != null) {
                            newUser.setRefreshToken(finalRefreshToken);
                            newUser.setRefreshTokenCreatedAt(LocalDateTime.now());
                        }
                        return newUser;
                    });

            user.setName(name);
            user.setPicture(picture);
            userRepository.save(user);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(CustomOAuth2UserService.class).error("사용자 저장 중 오류 발생: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("사용자 정보 저장 실패");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(user.getRole().getKey()));

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(
                authorities,
                attributes,
                userNameAttributeName);
    }
}
