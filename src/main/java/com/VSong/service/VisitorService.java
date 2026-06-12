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

    public VisitorService(DailyVisitorRepository visitorRepository) {
        this.visitorRepository = visitorRepository;
    }

    @Transactional
    public void incrementVisitorCount() {
        visitorRepository.incrementVisitorCount(LocalDate.now());
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
