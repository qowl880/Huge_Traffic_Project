package com.traffic.couponservice.service.v1;

import com.traffic.couponservice.config.UserIdInterceptor;
import com.traffic.couponservice.domain.Coupon;
import com.traffic.couponservice.domain.CouponPolicy;
import com.traffic.couponservice.dto.v1.CouponDto;
import com.traffic.couponservice.exception.CouponIssueException;
import com.traffic.couponservice.exception.CouponNotFoundException;
import com.traffic.couponservice.repository.CouponPolicyRepository;
import com.traffic.couponservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional
    public Coupon issueCoupon(CouponDto.IssueRequest request) {
        CouponPolicy couponPolicy = couponPolicyRepository.findByIdWithLock(request.getCouponPolicyId())
                .orElseThrow(() -> new CouponIssueException("쿠폰 정책을 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
            throw new CouponIssueException("쿠폰 발급 기간이 아닙니다.");
        }

        long issuedCouponCount = couponRepository.countByCouponPolicyId(couponPolicy.getId());
        if (issuedCouponCount >= couponPolicy.getTotalQuantity()) {
            throw new CouponIssueException("쿠폰이 모두 소진되었습니다.");
        }

        Coupon coupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .userId(UserIdInterceptor.getCurrentUserId())
                .couponCode(generateCouponCode())
                .build();

        return couponRepository.save(coupon);
    }

    // 쿠폰 코드를 통해 사용하기도 하기에 쿠폰 랜덤값 부여
    private String generateCouponCode(){
        return UUID.randomUUID().toString().substring(0,8);
    }

    // 쿠폰 사용
    @Transactional
    public Coupon useCoupon(Long couponId, Long orderId) {
        Long currentUserID = UserIdInterceptor.getCurrentUserId();
        Coupon coupon = couponRepository.findByIdAndUserId(couponId,currentUserID)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없거나 접근 권한이 없습니다."));

        coupon.use(orderId);
        return coupon;
    }

    // 쿠폰 취소
    @Transactional
    public Coupon cancelCoupon(Long couponId) {
        Long currentUserID = UserIdInterceptor.getCurrentUserId();
        Coupon coupon = couponRepository.findByIdAndUserId(couponId,currentUserID)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없거나 접근 권한이 없습니다."));

        coupon.cancel();
        return coupon;
    }


    @Transactional(readOnly =true)
    public Page<Coupon> getCoupons(CouponDto.ListRequest request){
        Long currentUserID = UserIdInterceptor.getCurrentUserId();
        return couponRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                currentUserID,
                request.getStatus(),
                PageRequest.of(
                        request.getPage() != null ? request.getPage() : 0,
                        request.getSize() != null ? request.getSize() : 10
                )
        );
    }
}
