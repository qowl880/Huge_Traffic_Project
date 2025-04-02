package com.traffic.couponservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_policies")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;        // 쿠폰 이름

    @Column
    private String description;     // 쿠폰 설명

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;      // 쿠폰 타입

    @Column(nullable = false)
    private Integer discountValue;          // 얼마나 할인이 들어가는지

    @Column(nullable = false)
    private Integer minimumOrderAmount;   // 최소 주문 금액

    @Column(nullable = false)
    private Integer maximumDiscountAmount;   // 최대 할인 금액

    @Column(nullable = false)
    private Integer totalQuantity;  // 총 몇개의 쿠폰을 발급할 것인지

    @Column(nullable = false)
    private LocalDateTime startTime;    // 쿠폰 사용 가능 시작일자

    @Column(nullable = false)
    private LocalDateTime endTime;    // 쿠폰 만료 일자

    @Column(nullable = false)
    private LocalDateTime createdAt;    // 쿠폰 생성 일자

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum DiscountType{
        FIXED_AMOUNT,       // 정액 할인
        PERCENTAGE          // 정률 할인
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
