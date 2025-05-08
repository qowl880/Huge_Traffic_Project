package com.traffic.pointservicebatch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableBatchProcessing
public class PointServiceBatchApplication {

	private final JobLauncher jobLauncher;		// 배치 작업을 실행하는 인터페이스
	private final Job pointBalanceSyncJob;		// 실행할 배치 작업

	public PointServiceBatchApplication(JobLauncher jobLauncher, Job pointBalanceSyncJob) {
		this.jobLauncher = jobLauncher;
		this.pointBalanceSyncJob = pointBalanceSyncJob;
	}

	public static void main(String[] args) {
		SpringApplication.run(PointServiceBatchApplication.class, args);
	}

	// ApplicationRunner : 애플리케이션 시작 시 자동으로 실행

	@Bean
	public ApplicationRunner runner() {
		return args -> {
			// 주입받은 pointBalanceSyncJob을 실행
			jobLauncher.run(
					pointBalanceSyncJob,
					new JobParametersBuilder()				//	timestamp를 추가해 매 실행마다 고유한 파라미터를 전달
							.addLong("timestamp", System.currentTimeMillis())
							.toJobParameters()
			);
		};
	}

}
