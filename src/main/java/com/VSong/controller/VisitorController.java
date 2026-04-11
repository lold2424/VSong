package com.VSong.controller;

import com.VSong.dto.MonthlyVisitorStats;
import com.VSong.entity.DailyVisitor;
import com.VSong.service.VisitorService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
public class VisitorController {

    private final VisitorService visitorService;
    private static final String VISITOR_COOKIE_NAME = "VSong-Visitor";

    public VisitorController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @PostMapping("/api/track-visit")
    public void trackVisit(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        boolean hasVisited = false;
        if (cookies != null) {
            hasVisited = Arrays.stream(cookies)
                               .anyMatch(c -> VISITOR_COOKIE_NAME.equals(c.getName()));
        }

        if (!hasVisited) {
            visitorService.incrementVisitorCount();

            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.getZone());
            long secondsUntilMidnight = Duration.between(now, midnight).getSeconds();

            Cookie visitorCookie = new Cookie(VISITOR_COOKIE_NAME, "true");
            visitorCookie.setMaxAge((int) secondsUntilMidnight);
            visitorCookie.setPath("/");
            response.addCookie(visitorCookie);
        }
    }

    @GetMapping("/api/visitors/daily")
    public List<DailyVisitor> getDailyVisitors() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);
        return visitorService.getDailyVisitorStats(startDate, endDate);
    }

    @GetMapping("/api/visitors/monthly")
    public List<MonthlyVisitorStats> getMonthlyVisitors() {
        return visitorService.getMonthlyVisitorStats();
    }

    @GetMapping("/api/visitors/total")
    public long getTotalVisitors() {
        return visitorService.getDailyVisitorStats(LocalDate.of(2000, 1, 1), LocalDate.now())
                .stream()
                .mapToLong(DailyVisitor::getCount)
                .sum();
    }
}
