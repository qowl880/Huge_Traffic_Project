package com.traffic.pointservice.repository;

import com.traffic.pointservice.domain.PointBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointBalanceRepository extends JpaRepository<PointBalance, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    Optional<PointBalance> findByUserId(Long userId);



}
