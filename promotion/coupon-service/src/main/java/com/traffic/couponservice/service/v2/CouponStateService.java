package com.traffic.couponservice.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.couponservice.domain.Coupon;
import com.traffic.couponservice.dto.v1.CouponDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponStateService {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String COUPON_STATE_KEY = "coupon:state:";

    /**

     */
    public void updateCouponState(Coupon coupon) {
        try {
            String stateKey = COUPON_STATE_KEY + coupon.getId();
            String couponJson = objectMapper.writeValueAsString(CouponDto.Response.from(coupon));
            RBucket<String> bucket = redissonClient.getBucket(stateKey);
            bucket.set(couponJson);

            log.info("Coupon state updated: {}", coupon.getId());

        } catch (Exception e) {
            log.error("Error updating coupon state: {}", e.getMessage(), e);
            throw new RuntimeException("쿠폰 상태 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    /**
        현재 발급된 쿠폰 데이터 가져옴 (사용한 쿠폰인지 아닌지)
     */
    public CouponDto.Response getCouponState(Long couponId) {
        try {
            String stateKey = COUPON_STATE_KEY + couponId;
            RBucket<String> bucket = redissonClient.getBucket(stateKey);
            String couponJson = bucket.get();

            if (couponJson == null) {
                return null;
            }

            return objectMapper.readValue(couponJson, CouponDto.Response.class);
        } catch (Exception e) {
            log.error("Error getting coupon state: {}", e.getMessage(), e);
            throw new RuntimeException("쿠폰 상태 조회 중 오류가 발생했습니다.", e);
        }
    }
}