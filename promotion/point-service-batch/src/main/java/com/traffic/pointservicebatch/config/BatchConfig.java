package com.traffic.pointservicebatch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableBatchProcessing
@EnableTransactionManagement        // 배치 처리 중 데이터베이스 작업 등에서 트랜잭션 처리가 필요할 때 사용합니다.
public class BatchConfig {
}
