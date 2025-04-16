package com.traffic.couponservice.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)     // 이 애노테이션을 메서드에만 적용할 수 있습니다.
@Retention(RetentionPolicy.RUNTIME)     // 애노테이션 정보가 런타임까지 유지됩니다.
public @interface CouponMetered {       // @interface: 커스텀 애노테이션을 정의합니다. 어노테이션에 대해 정의
    String version() default "v1";
}
