package com.VSong.repository;

import com.VSong.dto.MonthlyVisitorStats;
import com.VSong.entity.DailyVisitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyVisitorRepository extends JpaRepository<DailyVisitor, Long> {
    Optional<DailyVisitor> findByVisitDate(LocalDate visitDate);

    List<DailyVisitor> findAllByVisitDateBetweenOrderByVisitDateAsc(LocalDate startDate, LocalDate endDate);


    @Query("SELECT new com.VSong.dto.MonthlyVisitorStats(YEAR(dv.visitDate), MONTH(dv.visitDate), SUM(dv.count)) " +
           "FROM DailyVisitor dv " +
           "GROUP BY YEAR(dv.visitDate), MONTH(dv.visitDate) " +
           "ORDER BY YEAR(dv.visitDate) DESC, MONTH(dv.visitDate) DESC")
    List<MonthlyVisitorStats> findMonthlyVisitorStats();
}
