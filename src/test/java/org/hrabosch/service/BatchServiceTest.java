package org.hrabosch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private JobExplorer jobExplorer;
    @Mock
    private JobLauncher jobLauncher;
    @Mock
    private Job csvImportJob;

    private BatchService batchService;

    private static final String ERROR_MSG = "ERR MSG";
    private static final Long JOB_ID = 1L;
    private static final Long JOB_EXEC_ID = 2L;
    private static final String JOB_NAME = "JOB";

    @BeforeEach
    void setup() {
        this.batchService = new BatchService(jobExplorer, jobLauncher, csvImportJob);
    }

    @Test
    void testTriggeringReload() throws JobExecutionException {
        when(jobLauncher.run(any(), any())).thenReturn(new JobExecution(JOB_ID));

        batchService.triggerReload();

        verify(jobLauncher, times(1)).run(any(), any());
    }

    @Test
    void testGetLastBatchJobExecution() throws NoSuchJobInstanceException {
        JobInstance jobInstance = new JobInstance(JOB_ID, JOB_NAME);
        JobExecution jobExec = new JobExecution(JOB_EXEC_ID);
        jobExec.setJobInstance(jobInstance);

        when(jobExplorer.getLastJobInstance(anyString())).thenReturn(jobInstance);
        when(jobExplorer.getLastJobExecution(jobInstance)).thenReturn(jobExec);

        JobExecution jobExecution = batchService.getLastBatchProcess();

        assertEquals(JOB_EXEC_ID, jobExecution.getId());
        assertEquals(JOB_ID, jobExecution.getJobId());
        assertEquals(JOB_NAME, jobExecution.getJobInstance().getJobName());
    }

    @Test
    void testGetLastBatchJobExecutionWithoutJobExecution() {
        when(jobExplorer.getLastJobInstance(anyString())).thenReturn(null);

        assertThrows(NoSuchJobInstanceException.class, () -> {
            batchService.getLastBatchProcess();
        });
    }
}
