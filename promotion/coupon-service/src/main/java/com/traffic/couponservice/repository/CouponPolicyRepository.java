package com.traffic.couponservice.repository;

import com.traffic.couponservice.domain.CouponPolicy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)                           // 비관적 락 - 잠금이 설정된 동안에는 현재 업무가 끝나기 전까지 다른 api가 들어와도 대기상대가 됨
    @Query("SELECT cp FROM CouponPolicy cp WHERE cp.id = :id")
    Optional<CouponPolicy> findByIdWithLock(Long id);
}