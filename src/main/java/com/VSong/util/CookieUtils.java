package com.VSong.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.SerializationUtils;

import java.io.Serializable;
import java.util.Base64;
import java.util.Optional;

public class CookieUtils {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }

        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        boolean isSecure = value != null && !value.isEmpty();
        
        org.springframework.http.ResponseCookie.ResponseCookieBuilder cookieBuilder = org.springframework.http.ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .maxAge(maxAge);

        if (!"localhost".equals(System.getenv("NODE_ENV")) && !"local".equals(System.getProperty("spring.profiles.active"))) {
            cookieBuilder.secure(true).sameSite("None");
        }

        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        org.springframework.http.ResponseCookie.ResponseCookieBuilder cookieBuilder = org.springframework.http.ResponseCookie.from(name, "")
                .path("/")
                .httpOnly(true)
                .maxAge(0);

        if (!"localhost".equals(System.getenv("NODE_ENV")) && !"local".equals(System.getProperty("spring.profiles.active"))) {
            cookieBuilder.secure(true).sameSite("None");
        }

        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

    public static String serialize(Object object) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(object));
    }

    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                        Base64.getUrlDecoder().decode(cookie.getValue())));
    }
}
