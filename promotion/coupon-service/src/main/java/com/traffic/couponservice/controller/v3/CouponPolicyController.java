package com.traffic.couponservice.controller.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.traffic.couponservice.dto.v3.CouponPolicyDto;
import com.traffic.couponservice.service.v3.CouponPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController("couponPolicyControllerV3")
@RequestMapping("/api/v3/coupon-policies")
@RequiredArgsConstructor
public class CouponPolicyController {
    private final CouponPolicyService couponPolicyService;

    // 쿠폰 정책 생성
    @PostMapping
    public ResponseEntity<CouponPolicyDto.Response> addCouponPolicy(@RequestBody CouponPolicyDto.CreateRequest request) throws JsonProcessingException {
        return ResponseEntity.ok()
                .body(CouponPolicyDto.Response.from(couponPolicyService.createCouponPolicy(request)));
    }

    // 특정 쿠폰 정책 호출
    @GetMapping("/{id}")
    public ResponseEntity<CouponPolicyDto.Response> getCouponPolicy(@PathVariable Long id) {
        return ResponseEntity.ok(CouponPolicyDto.Response.from(couponPolicyService.getCouponPolicy(id)));
    }

    // 모든 쿠폰 정책 호출
    @GetMapping
    public ResponseEntity<List<CouponPolicyDto.Response>> getAllCouponPolicies(){
        return ResponseEntity.ok(couponPolicyService.getAllCouponPolicies().stream()
                .map(CouponPolicyDto.Response::from)
                .collect(Collectors.toList()));
    }
}
