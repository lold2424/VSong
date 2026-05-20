package com.VSong.repository;

import com.VSong.entity.VisitorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface VisitorLogRepository extends JpaRepository<VisitorLog, Long> {
    boolean existsByIpAndVisitDate(String ip, LocalDate visitDate);
}
