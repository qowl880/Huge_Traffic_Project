package com.traffic.couponservice.exception;

public class CouponPolicyNotFoundException extends RuntimeException{
    public CouponPolicyNotFoundException(String message){
        super(message);
    }
}
