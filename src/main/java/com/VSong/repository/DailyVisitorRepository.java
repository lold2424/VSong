package com.VSong.repository;

import com.VSong.dto.MonthlyVisitorStats;
import com.VSong.entity.DailyVisitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyVisitorRepository extends JpaRepository<DailyVisitor, Long> {
    Optional<DailyVisitor> findByVisitDate(LocalDate visitDate);

    List<DailyVisitor> findAllByVisitDateBetweenOrderByVisitDateAsc(LocalDate startDate, LocalDate endDate);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO daily_visitor (visit_date, count) VALUES (:visitDate, 1) " +
                   "ON DUPLICATE KEY UPDATE count = count + 1", nativeQuery = true)
    void incrementVisitorCount(@Param("visitDate") LocalDate visitDate);

    @Query("SELECT new com.VSong.dto.MonthlyVisitorStats(YEAR(dv.visitDate), MONTH(dv.visitDate), SUM(dv.count)) " +
           "FROM DailyVisitor dv " +
           "GROUP BY YEAR(dv.visitDate), MONTH(dv.visitDate) " +
           "ORDER BY YEAR(dv.visitDate) DESC, MONTH(dv.visitDate) DESC")
    List<MonthlyVisitorStats> findMonthlyVisitorStats();

    @Modifying
    @Query("DELETE FROM DailyVisitor d WHERE d.visitDate < :thresholdDate")
    void deleteOldVisitors(@Param("thresholdDate") LocalDate thresholdDate);
}
