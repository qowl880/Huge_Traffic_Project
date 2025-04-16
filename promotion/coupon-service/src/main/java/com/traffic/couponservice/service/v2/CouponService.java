package com.traffic.couponservice.service.v2;

import com.traffic.couponservice.domain.Coupon;
import com.traffic.couponservice.dto.v1.CouponDto;
import com.traffic.couponservice.exception.CouponNotFoundException;
import com.traffic.couponservice.repository.CouponRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("couponServiceV2")
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final CouponRedisService couponRedisService;
    private final CouponStateService couponStateService;

    // 쿠폰 발급
    @Transactional
    public CouponDto.Response issueCoupon(CouponDto.IssueRequest request){

        Coupon coupon = couponRedisService.issueCoupon(request);
        couponStateService.updateCouponState(couponRepository.findById(coupon.getId())
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없습니다.")));
        
        return CouponDto.Response.from(coupon);
    }
    
    // 쿠폰 사용
    @Transactional
    public CouponDto.Response useCoupon(Long couponId, Long orderId){
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없습니다."));

        coupon.use(orderId);
        // 쿠폰 상태값을 use로 변경
        couponStateService.updateCouponState(coupon);

        return CouponDto.Response.from(coupon);
    }

    // 쿠폰 취소
    @Transactional
    public CouponDto.Response cancelCoupon(Long couponId){
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없습니다."));

        coupon.cancel();
        // 쿠폰 상태값을 use로 변경
        couponStateService.updateCouponState(coupon);

        return CouponDto.Response.from(coupon);
    }

    // 쿠폰 조회
    public CouponDto.Response getCoupon(Long couponId){
        // 현재 쿠폰 값이 Redis에 저장되어 있다면 해당 값 가져옴
        CouponDto.Response cachedCoupon = couponStateService.getCouponState(couponId);
        if(cachedCoupon != null){
            return cachedCoupon;
        }
        
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없습니다."));
        
        CouponDto.Response response = CouponDto.Response.from(coupon);
        couponStateService.updateCouponState(coupon);
        
        return response;
    }
}
