package com.VSong.service;

import com.VSong.dto.MonthlyVisitorStats;
import com.VSong.entity.DailyVisitor;
import com.VSong.repository.DailyVisitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class VisitorService {

    private final DailyVisitorRepository visitorRepository;
    private final com.VSong.repository.VisitorLogRepository visitorLogRepository;

    public VisitorService(DailyVisitorRepository visitorRepository,
                          com.VSong.repository.VisitorLogRepository visitorLogRepository) {
        this.visitorRepository = visitorRepository;
        this.visitorLogRepository = visitorLogRepository;
    }

    @Transactional
    public void trackVisitor(String ip, LocalDate date) {
        if (!visitorLogRepository.existsByIpAndVisitDate(ip, date)) {
            visitorRepository.incrementVisitorCount(date);
            
            com.VSong.entity.VisitorLog log = new com.VSong.entity.VisitorLog();
            log.setIp(ip);
            log.setVisitDate(date);
            log.setLastVisitTime(java.time.LocalDateTime.now());
            visitorLogRepository.save(log);
        }
    }

    @Transactional
    public void incrementVisitorCount(LocalDate date) {
        visitorRepository.incrementVisitorCount(date);
    }

    @Transactional(readOnly = true)
    public List<DailyVisitor> getDailyVisitorStats(LocalDate startDate, LocalDate endDate) {
        return visitorRepository.findAllByVisitDateBetweenOrderByVisitDateAsc(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<MonthlyVisitorStats> getMonthlyVisitorStats() {
        return visitorRepository.findMonthlyVisitorStats();
    }
}
