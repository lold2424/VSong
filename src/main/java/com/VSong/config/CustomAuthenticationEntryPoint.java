package com.VSong.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;

import java.io.IOException;

public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // "X-Requested-With" 헤더가 "XMLHttpRequest"인 경우 AJAX 요청으로 간주합니다.
        boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

        if (isAjax) {
            // AJAX 요청일 경우, 401 Unauthorized 상태 코드를 응답합니다.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized: Authentication is required.");
        } else {
            // 일반 브라우저 요청일 경우, OAuth2 로그인 흐름을 시작합니다.
            // Google 로그인 엔드포인트로 리다이렉션합니다.
            redirectStrategy.sendRedirect(request, response, "/oauth2/authorization/google");
        }
    }
}