package com.VSong.service;

import com.VSong.entity.User;
import com.VSong.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        // Google은 첫 승인 시에만 refresh token을 발급하므로, access token을 대신 저장하거나 null을 허용해야 합니다.
        // 여기서는 access token을 저장하겠습니다.
        String accessToken = userRequest.getAccessToken().getTokenValue();

        User user = userRepository.findByEmail(email)
                .map(entity -> {
                    return entity;
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setRole(com.VSong.entity.Role.USER); // 기본 역할 부여
                    return newUser;
                });

        user.setEmail(email);
        user.setName(name);
        user.setPicture(picture);
        user.setRefreshToken(accessToken);
        user.setTokenCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user);

        return oAuth2User;
    }
}
