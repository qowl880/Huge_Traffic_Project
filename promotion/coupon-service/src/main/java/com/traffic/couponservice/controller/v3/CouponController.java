package com.traffic.couponservice.controller.v3;

import com.traffic.couponservice.dto.v3.CouponDto;
import com.traffic.couponservice.service.v3.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("couponControllerV3")
@RequestMapping("/api/v3/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    // 쿠폰 발급 요청
    @PostMapping("/issue")
    public ResponseEntity<CouponDto.Response> issueCoupon(@RequestBody CouponDto.IssueRequest request) {
        couponService.requestCouponIssue(request);
        return ResponseEntity.accepted().build();
    }

    // 쿠폰 사용
    @PostMapping("/{couponId}/use")
    public ResponseEntity<CouponDto.Response> useCoupon(
            @PathVariable Long couponId,
            @RequestParam Long orderId
    ) {
        CouponDto.Response response = CouponDto.Response.from(couponService.useCoupon(couponId, orderId));
        return ResponseEntity.ok(response);
    }

    // 쿠폰 사용 취소
    @PostMapping("/{couponId}/cancel")
    public ResponseEntity<Void> cancelCoupon(@PathVariable Long couponId) {
        CouponDto.Response response = CouponDto.Response.from(couponService.cancelCoupon(couponId));
        return ResponseEntity.ok().build();
    }


}
