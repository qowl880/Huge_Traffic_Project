package com.traffic.pointservicebatch.repository;

import com.traffic.pointservicebatch.domain.PointBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointBalanceRepository extends JpaRepository<PointBalance, Long> {
}
