package com.traffic.couponservice.aop;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect     // Aspect가 메서드 실행을 가로챔
@Component
@RequiredArgsConstructor
public class CouponMetricsAspect {
    private final MeterRegistry registry;

    // @Around 어드바이스가 @CouponMetered가 붙은 메서드를 감싸 실행 전후에 메트릭을 기록
    @Around("@annotation(CouponMetered)")       //
    public Object measureCouponOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start();        // Timer.Sample로 실행 시간 측정 시작.
        String version = extractVersion(joinPoint);
        String operation = extractOperation(joinPoint);

        try {
            // joinPoint.proceed()로 실제 비즈니스 로직(CouponsService의 Issue메서드) 실행.
            Object result = joinPoint.proceed();

            // 쿠폰 발급 성공 메트릭
            // 카운터 증가
            Counter.builder("coupon.operation.success")
                    .tag("version", version)
                    .tag("operation", operation)
                    .register(registry)
                    .increment();

            // 타이머 종료
            sample.stop(Timer.builder("coupon.operation.duration")
                    .tag("version", version)
                    .tag("operation", operation)
                    .register(registry));

            return result;
        } catch (Exception e) {
            // 쿠폰 발급 실패 메트릭
            // 카운터 증가 (에러 유형 태그 포함).
            Counter.builder("coupon.operation.failure")
                    .tag("version", version)
                    .tag("operation", operation)
                    .tag("error", e.getClass().getSimpleName())
                    .register(registry)
                    .increment();
            throw e;
        }
    }

    private String extractVersion(ProceedingJoinPoint joinPoint) {
        CouponMetered annotation = ((MethodSignature) joinPoint.getSignature())
                .getMethod()
                .getAnnotation(CouponMetered.class);
        return annotation.version();
    }

    private String extractOperation(ProceedingJoinPoint joinPoint) {
        return joinPoint.getSignature().getName();
    }
}
