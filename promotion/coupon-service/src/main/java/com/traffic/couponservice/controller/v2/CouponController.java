package com.traffic.couponservice.controller.v2;

import com.traffic.couponservice.dto.v1.CouponDto;
import com.traffic.couponservice.service.v2.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("couponControllerV2")
@RequestMapping("/api/v2/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    // 쿠폰 발급 요청
    @PostMapping("/issue")
    public ResponseEntity<CouponDto.Response> issueCoupon(@RequestBody CouponDto.IssueRequest request) {
        return ResponseEntity.ok(couponService.issueCoupon(request));
    }
    
    // 쿠폰 사용
    @PostMapping("/{couponId}/use")
    public ResponseEntity<CouponDto.Response> useCoupon(@PathVariable Long couponId, @RequestBody CouponDto.UseRequest request) {
        return ResponseEntity.ok(couponService.useCoupon(couponId, request.getOrderId()));
    }
    
    // 쿠폰 사용 취소
    @PostMapping("/{couponId}/cancel")
    public ResponseEntity<CouponDto.Response> cancelCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.cancelCoupon(couponId));
    }
    
    
}
