package com.traffic.pointservicebatch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

// Batch 실행 전, 실행 후의 동작 기능을 담당하는 곳 (예. Slack을 통한 알림 전송)
@Component
@Slf4j
public class JobCompletionNotificationListener implements JobExecutionListener {

    // Batch Job 실행 전
    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job {} is starting...", jobExecution.getJobInstance().getJobName());
    }


    // Batch Job 실행 후
    @Override
    public void afterJob(JobExecution jobExecution) {
        if(jobExecution.getStatus() == BatchStatus.COMPLETED){
            log.info("JOb {} completed successfully", jobExecution.getJobInstance().getJobName());
        }else{
            log.error("Job {} failed with status {}",
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getStatus());
        }
    }
}
