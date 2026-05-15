package com.VSong.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
public class LogoutController {

    @org.springframework.beans.factory.annotation.Value("${oauth2.success.redirect-url}")
    private String redirectUrl;

    @GetMapping("/api/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        request.logout();

        response.sendRedirect(redirectUrl + "?logout=success");
    }
}
