package org.hrabosch.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.hrabosch.model.CryptoPrice;
import org.hrabosch.model.CryptoPriceComputed;
import org.hrabosch.model.CryptoPriceStatistics;
import org.hrabosch.service.CryptoRecsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/crypto")
public class CryptoRecsController {

    private CryptoRecsService cryptoRecsService;

    @Autowired
    public CryptoRecsController(CryptoRecsService cryptoRecsService) {
        this.cryptoRecsService = cryptoRecsService;
    }

    @Operation(summary = "List all allowed price records for all or specific symbol.")
    @GetMapping("/list")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<CryptoPrice>> getAvailableCryptos(@RequestParam Optional<String> symbol) {
        List<CryptoPrice> results;
        results = symbol.isPresent() ? cryptoRecsService.findBySymbol(symbol.get()) : cryptoRecsService.findAll();
        return results.isEmpty()
                ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                : new ResponseEntity<>(results, HttpStatus.OK);
    }

    @Operation(summary = "Get normalized price per stored crypto.",
            description = "Computing normalized price per allowed symbol stored within service."
                    + "Normalized price is calculated from all available data.")
    @GetMapping("/normalized/all")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<CryptoPriceComputed>> getAllNormalized(
            @RequestParam(required = false,
                    defaultValue = "DESC") Sort.Direction sort) {
        List<CryptoPriceComputed> results = cryptoRecsService.getAllNormalized(sort);
        return CollectionUtils.isEmpty(results)
                ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                : new ResponseEntity<>(results, HttpStatus.OK);
    }

    @Operation(summary = "Get highest normalized of day.",
            description = "Computes and returns Crypto with highest normalized price for specific day.")
    @GetMapping("/normalized/top")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<CryptoPriceComputed> getHighestNormalizedForDay(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Optional<CryptoPriceComputed> result = cryptoRecsService.getHighestNormalize(date);
        return result.isPresent() ? new ResponseEntity<>(result.get(), HttpStatus.OK)
                : new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Find oldest/newest/min/max entry for given month.")
    @GetMapping(value = {"/statistics", "/statistics/{symbol}"})
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<CryptoPriceStatistics>> getStatistics(
            @PathVariable Optional<String> symbol,
            @Parameter(example = "2022-01-08")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Optional<LocalDate> yearMonth) {
        List<CryptoPriceStatistics> results = cryptoRecsService.getStatistics(symbol, yearMonth);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @Operation(summary = "Find oldest/newest/min/max entry for given time period.")
    @GetMapping(value = {"/range-statistics", "/range-statistics/{symbol}"})
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<CryptoPriceStatistics>> getStatisticsForTimeRange(
            @PathVariable Optional<String> symbol,
            @Parameter(example = "2022-01-08 21:00:00")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @Parameter(example = "2022-01-08 21:00:00")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to) {
        List<CryptoPriceStatistics> results = cryptoRecsService.getStatisticsForTimeRange(symbol, from, to);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

}
