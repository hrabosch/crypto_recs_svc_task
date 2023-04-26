package org.hrabosch.controller;

import org.hrabosch.service.BatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchControllerTest {

    @Mock
    private BatchService batchService;
    private BatchController batchController;

    private static final String ERROR_MESSAGE = "ERROR";
    private static final Long JOB_EXECUTION_ID = 258L;

    @BeforeEach
    void setUp() {
        batchController = new BatchController(batchService);
    }

    @Test()
    void refreshDataException() throws JobExecutionException {
        doThrow(new JobExecutionException(ERROR_MESSAGE)).when(batchService).triggerReload();

        ResponseEntity<HttpStatus> responseEntity = batchController.refreshData();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }

    @Test
    void refreshDataTriggeredSuccessfully() throws JobExecutionException {
        doNothing().when(batchService).triggerReload();

        ResponseEntity<HttpStatus> responseEntity = batchController.refreshData();

        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
    }

    @Test
    void lastBatchExecStatusSuccess() throws NoSuchJobInstanceException {
        when(batchService.getLastBatchProcess()).thenReturn(new JobExecution(JOB_EXECUTION_ID));

        ResponseEntity<JobExecution> responseEntity = batchController.lastBatchExecStatus();

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    void lastBatchExecStatusNoJobException() throws NoSuchJobInstanceException {
        when(batchService.getLastBatchProcess()).thenThrow(new NoSuchJobInstanceException(ERROR_MESSAGE));

        ResponseEntity<JobExecution> responseEntity = batchController.lastBatchExecStatus();

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }
}
