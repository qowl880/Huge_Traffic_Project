package com.traffic.couponservice.controller.v1;

import com.traffic.couponservice.domain.Coupon;
import com.traffic.couponservice.dto.v1.CouponDto;
import com.traffic.couponservice.service.v1.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/issue")
    public ResponseEntity<CouponDto.Response> issueCoupon(@RequestBody CouponDto.IssueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CouponDto.Response.from(couponService.issueCoupon(request)));
    }

    @PostMapping("/{couponId}/use")
    public ResponseEntity<CouponDto.Response> useCoupon(
            @PathVariable Long couponId,
            @RequestBody CouponDto.UseRequest request) {
        return ResponseEntity.ok(CouponDto.Response.from(couponService.useCoupon(couponId, request.getOrderId())));
    }

    @PostMapping("/{couponId}/cancel")
    public ResponseEntity<CouponDto.Response> cancelCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(CouponDto.Response.from(couponService.cancelCoupon(couponId)));
    }

    @GetMapping
    public ResponseEntity<List<CouponDto.Response>> getCoupons(
            @RequestParam(required = false) Coupon.Status status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        CouponDto.ListRequest request = CouponDto.ListRequest.builder()
                .status(status)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(couponService.getCoupons(request).stream()
                .map(CouponDto.Response :: from)
                .toList());
    }

}
