package com.traffic.pointservicebatch.repository;

import com.traffic.pointservicebatch.domain.DailyPointReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyPointReportRepository extends JpaRepository<DailyPointReport, Integer> {
    List<DailyPointReport> findByReportDate(LocalDate reportDate);
    List<DailyPointReport> findByUserIdAndReportDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
