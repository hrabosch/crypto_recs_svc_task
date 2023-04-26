package org.hrabosch.service;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BatchService {
    private JobExplorer jobExplorer;
    private JobLauncher jobLauncher;
    private Job csvImportJob;

    @Autowired
    public BatchService(JobExplorer jobExplorer, JobLauncher jobLauncher, Job csvImportJob) {
        this.jobExplorer = jobExplorer;
        this.jobLauncher = jobLauncher;
        this.csvImportJob = csvImportJob;
    }

    public void triggerReload() throws JobExecutionException {
        jobLauncher.run(csvImportJob, new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .toJobParameters());
    }

    public JobExecution getLastBatchProcess() throws NoSuchJobInstanceException {
        JobInstance lastJobInstance = jobExplorer.getLastJobInstance("csvImportJob");
        if (lastJobInstance == null) {
            throw new NoSuchJobInstanceException("Cannot find CSV Import Job.");
        }
        return jobExplorer.getLastJobExecution(lastJobInstance);
    }
}
