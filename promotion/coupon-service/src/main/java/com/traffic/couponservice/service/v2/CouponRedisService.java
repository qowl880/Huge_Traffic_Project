package com.traffic.couponservice.service.v2;

import com.traffic.couponservice.aop.CouponMetered;
import com.traffic.couponservice.config.UserIdInterceptor;
import com.traffic.couponservice.domain.Coupon;
import com.traffic.couponservice.domain.CouponPolicy;
import com.traffic.couponservice.dto.v1.CouponDto;
import com.traffic.couponservice.exception.CouponIssueException;
import com.traffic.couponservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponRedisService {
    private final RedissonClient redissonClient;
    private final CouponRepository couponRepository;
    private final CouponPolicyService couponPolicyService;

    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";
    private static final String COUPON_LOCK_KEY = "coupon:lock:";
    private static final String COUPON_USER_ID = "coupon:userId:";
    private static final long LOCK_WAIT_TIME = 3;       // Lock 대기 시간 제한
    private static final long LOCK_LEASE_TIME = 5;      // 자동 만료 시간

    @Transactional
    @CouponMetered(version = "v2")
    public Coupon issueCoupon(CouponDto.IssueRequest request){
        String quantityKey = COUPON_QUANTITY_KEY + request.getCouponPolicyId();
        String lockKey = COUPON_LOCK_KEY + request.getCouponPolicyId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
//            // 이미 발급받은 유저인지 확인
            Long userId = UserIdInterceptor.getCurrentUserId();     // InterCepter를 통한 현재 Header에 저장되어 있는 UserId값 추출
//            RSet<Long> userIdSet = redissonClient.getSet(COUPON_USER_ID+userId);
//            if(userIdSet.contains(userId)){
//                throw new CouponIssueException("이미 발급받은 쿠폰입니다. 더 이상 발급이 불가능 합니다");
//            }

            // Redis에서 Lock 시간 설정
            // 여기서 락을 걸어주는 이유는 동시에 2명의 유저가 접속했을때 해당 쿠폰 정책을 동시에 읽게 되면 decrementAndGet 기능이
            // 동시에 발생하게 되어 -2가 생길 수도 있음 따라서, 쿠폰 정책데이터에 Lock 지정하여 다른 유저가 접근 못하게 막음
            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

            if (!isLocked) {
                throw new CouponIssueException("쿠폰 발급 요청이 많아 쿠폰 발급이 불가능 합니다. 잠시 후 다시 시도해주세요");
            }

            CouponPolicy couponPolicy = couponPolicyService.getCouponPolicy(request.getCouponPolicyId());

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
                throw new IllegalStateException("쿠폰 발급 기간이 아닙니다.");
            }

            // 쿠폰 사용하여 수량 체크 및 감소
            RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
            long remainingQuantity = atomicQuantity.decrementAndGet();

            // 발급할 쿠폰이 없다면
            if (remainingQuantity < 0) {
                // 음수의 값으로 유지하게 되면 오류가 발생할 수 있기에 다시 증가 시켜줌
                atomicQuantity.incrementAndGet();
                throw new CouponIssueException("쿠폰이 모두 소진되었습니다.");
            }

//            //  중복 유저 발급을 방지하기 위한 Redis Set에 UserId 저장
//            userIdSet.add(userId);

            // 쿠폰 발급
            return couponRepository.save(Coupon.builder()
                    .couponPolicy(couponPolicy)
                    .userId(userId)
                    .couponCode(generateCouponCode())
                    .build());
            }catch (InterruptedException e){
            // Lock인 상태에서 인터럽트 요청이 들어오면 해당 메서드는 즉시 종료되고 InterruptedException이 발생
            // InterruptedException이 발생하면 인터럽트 상태 플래그가 초기화되므로,
            // 이후 코드나 상위 호출 계층에서 더 이상 "이 스레드가 인터럽트되었는지"를 알 수 없습니다.
            // 따라서 아래의 코드를 사용하여 복원해줘야 함
                Thread.currentThread().interrupt();
                throw new CouponIssueException("쿠폰 발급 중 오류가 발생했습니다");
            }finally{
                if(lock.isHeldByCurrentThread())        // 현재 스레드가 락을 보유하지 않았을 때 unlock() 호출 시 발생하는 IllegalMonitorStateException 방지
                    lock.unlock();      // 모든 작업이 끝났다면 Lock 해제
            }
    }

    private String generateCouponCode() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}