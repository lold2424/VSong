package com.VSong.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
public class LogoutController {

    @GetMapping("/api/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 세션 무효화
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        request.logout();

        response.sendRedirect("http://localhost:3000?logout=success");
    }
}
