package org.hrabosch.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.hrabosch.service.BatchService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private BatchService batchService;

    @Autowired
    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }


    @Operation(summary = "Trigger CSV import batch job.")
    @PatchMapping("/refresh")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<HttpStatus> refreshData() {
        try {
            batchService.triggerReload();
        } catch (JobExecutionException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Operation(summary = "Get result of latest CSV import job execution.")
    @PatchMapping("/lastBatchExecStatus")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<JobExecution> lastBatchExecStatus() {
        try {
            JobExecution jobExecution = batchService.getLastBatchProcess();
            return new ResponseEntity<>(jobExecution, HttpStatus.OK);
        } catch (NoSuchJobInstanceException e) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

}
