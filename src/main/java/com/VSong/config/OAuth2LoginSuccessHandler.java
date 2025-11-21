package com.VSong.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public OAuth2LoginSuccessHandler(@Value("${oauth2.success.redirect-url}") String defaultTargetUrl) {
        // 로그인 성공 시 리다이렉션할 기본 URL을 설정하고,
        super.setDefaultTargetUrl(defaultTargetUrl);
        // 항상 이 URL을 사용하도록 설정합니다.
        super.setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 부모 클래스의 onAuthenticationSuccess를 호출하여 리다이렉션을 수행합니다.
        super.onAuthenticationSuccess(request, response, authentication); // 빠뜨렸던 authentication 파라미터 추가
    }
}