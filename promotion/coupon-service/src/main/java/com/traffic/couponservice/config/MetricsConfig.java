package com.traffic.couponservice.config;

import com.traffic.couponservice.aop.CouponMetricsAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.beans.BeanProperty;

@Configuration
@EnableAspectJAutoProxy
public class MetricsConfig {

    @Bean
    public CouponMetricsAspect couponMetricsAspect(MeterRegistry meterRegistry) {
        return new CouponMetricsAspect(meterRegistry);
    }
}
