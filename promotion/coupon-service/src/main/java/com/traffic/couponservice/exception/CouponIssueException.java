package com.traffic.couponservice.exception;

public class CouponIssueException extends RuntimeException{
    public CouponIssueException(String message){
        super(message);
    }

    public CouponIssueException(String message, Throwable cause){
        super(message,cause);
    }
}
