package com.traffic.couponservice.service.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.couponservice.domain.CouponPolicy;
import com.traffic.couponservice.dto.v3.CouponPolicyDto;
import com.traffic.couponservice.exception.CouponPolicyNotFoundException;
import com.traffic.couponservice.repository.CouponPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("couponPolicyService3")
@RequiredArgsConstructor
@Slf4j
public class CouponPolicyService {
    private final CouponPolicyRepository couponPolicyRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";
    private static final String COUPON_POLICY_KEY = "coupon:policy:";

    @Transactional
    public CouponPolicy createCouponPolicy(CouponPolicyDto.CreateRequest request) throws JsonProcessingException {
        CouponPolicy couponPolicy = request.toEntity();
        CouponPolicy savedPolicy = couponPolicyRepository.save(couponPolicy);

        // Redis에 초기 수량 설정
        String quantityKey = COUPON_QUANTITY_KEY + savedPolicy.getId();
        // 안전한 수량 측정을 위해 RAtomicLong 사용
        RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
        atomicQuantity.set(savedPolicy.getTotalQuantity());

        // Redis에 정책 정보 저장
        String policyKey = COUPON_POLICY_KEY + savedPolicy.getId();
        // Json 형식으로 직렬화
        // savedPolicy 객체를 CouponPolicyDto.Response로 직렬화
        String policyJson = objectMapper.writeValueAsString(CouponPolicyDto.Response.from(savedPolicy));
        // 객체를 Redis에 저장시키기 위해 RBucket 사용
        RBucket<String> bucket = redissonClient.getBucket(policyKey);
        bucket.set(policyJson);

        return savedPolicy;
    }

    public CouponPolicy getCouponPolicy(Long id){
        String policyKey = COUPON_POLICY_KEY + id;
        RBucket<String> bucket = redissonClient.getBucket(policyKey);

        String policyJson = bucket.get();

        if(policyJson != null){
            try{
                // Json 객체인 policyJson를 Java 객체인 CouponPolicy로 변환
                return objectMapper.readValue(policyJson, CouponPolicy.class);
            } catch (JsonProcessingException e) {
                log.error("쿠폰 정책 정보를 Json으로 파싱하는 도중 오류 발생");
            }
        }

        return couponPolicyRepository.findById(id)
                .orElseThrow(() -> new CouponPolicyNotFoundException("쿠폰 정책을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<CouponPolicy> getAllCouponPolicies(){
        return couponPolicyRepository.findAll();
    }
}
