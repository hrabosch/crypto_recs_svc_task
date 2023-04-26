package org.hrabosch.controller;

import org.hrabosch.model.CryptoPrice;
import org.hrabosch.model.CryptoPriceComputed;
import org.hrabosch.model.CryptoPriceStatistics;
import org.hrabosch.service.CryptoRecsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CryptoRecsControllerTest {

    @Mock
    private CryptoRecsService cryptoRecsService;

    private CryptoRecsController cryptoRecsController;

    public static final String[] SYMBOLS = {"AAA", "BBB", "CCC"};
    public static final Double PRICE = 1.123456;
    public static final Integer COUNT = 10;

    private static final LocalDateTime NOW = LocalDateTime.now();


    @BeforeEach
    void setUp() {
        cryptoRecsController = new CryptoRecsController(cryptoRecsService);
    }

    @Test
    void getAvailableCryptosAllSuccess() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        List<CryptoPrice> dummyCryptos = generateDummyData(COUNT, now, SYMBOLS[0], PRICE);
        dummyCryptos.addAll(generateDummyData(COUNT / 2, now, SYMBOLS[1], PRICE));

        when(cryptoRecsService.findAll()).thenReturn(dummyCryptos);

        ResponseEntity<List<CryptoPrice>> responseEntity = cryptoRecsController.getAvailableCryptos(Optional.empty());

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertFalse(responseEntity.getBody().isEmpty());
    }

    @Test
    void getAvailableCryptosNoContent() {
        when(cryptoRecsService.findAll()).thenReturn(Collections.emptyList());

        ResponseEntity<List<CryptoPrice>> responseEntity = cryptoRecsController
                .getAvailableCryptos(Optional.empty());

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody());
    }

    @Test
    void getAvailableCryptosForSymbolSuccess() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        List<CryptoPrice> dummyCryptos = generateDummyData(COUNT, now, SYMBOLS[0], PRICE);

        when(cryptoRecsService.findBySymbol(eq(SYMBOLS[0]))).thenReturn(dummyCryptos);

        ResponseEntity<List<CryptoPrice>> responseEntity = cryptoRecsController.getAvailableCryptos(Optional.of(SYMBOLS[0]));

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertFalse(responseEntity.getBody().isEmpty());
        assertEquals(COUNT, responseEntity.getBody().size());
    }

    @Test
    void getAvailableCryptosForSymbolNoContent() {

        when(cryptoRecsService.findBySymbol(eq(SYMBOLS[0]))).thenReturn(Collections.emptyList());

        ResponseEntity<List<CryptoPrice>> responseEntity = cryptoRecsController.getAvailableCryptos(Optional.of(SYMBOLS[0]));

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody());
    }

    @Test
    void getAllNormalizedDescSuccess() {
        List<CryptoPriceComputed> priceComputeds = generateNormalized(PRICE, SYMBOLS);

        when(cryptoRecsService.getAllNormalized(any())).thenReturn(priceComputeds);

        ResponseEntity<List<CryptoPriceComputed>> responseEntity = cryptoRecsController
                .getAllNormalized(Sort.Direction.DESC);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(SYMBOLS.length, responseEntity.getBody().size());
    }

    @Test
    void getAllNormalizedDescNoContent() {
        when(cryptoRecsService.getAllNormalized(any())).thenReturn(Collections.emptyList());

        ResponseEntity<List<CryptoPriceComputed>> responseEntity = cryptoRecsController
                .getAllNormalized(Sort.Direction.DESC);

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody());
    }

    @Test
    void getHighestNormalizedForDaySuccess() {
        List<CryptoPriceComputed> priceComputeds = generateNormalized(PRICE, SYMBOLS[0]);
        when(cryptoRecsService.getHighestNormalize(any(LocalDate.class)))
                .thenReturn(Optional.of(priceComputeds.get(0)));

        ResponseEntity<CryptoPriceComputed> responseEntity = cryptoRecsController
                .getHighestNormalizedForDay(LocalDate.now());

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(PRICE, responseEntity.getBody().getNormalized());
        assertEquals(SYMBOLS[0], responseEntity.getBody().getSymbol());
    }

    @Test
    void getHighestNormalizedForDayNoContent() {
        when(cryptoRecsService.getHighestNormalize(any(LocalDate.class)))
                .thenReturn(Optional.empty());

        ResponseEntity<CryptoPriceComputed> responseEntity = cryptoRecsController
                .getHighestNormalizedForDay(LocalDate.now());

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }

    @Test
    void getStatisticsForAll() {
        List<CryptoPriceStatistics> dummyStatistics = generateStatistics(PRICE, PRICE / 2, NOW.minusDays(2), NOW, SYMBOLS);

        when(cryptoRecsService.getStatistics(any(), any())).thenReturn(dummyStatistics);

        ResponseEntity<List<CryptoPriceStatistics>> responseEntity = cryptoRecsController
                .getStatistics(Optional.empty(), Optional.empty());

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(SYMBOLS.length, responseEntity.getBody().size());
    }

    @Test
    void getStatisticsForTimeRange() {
        List<CryptoPriceStatistics> dummyStatistics = generateStatistics(PRICE, PRICE / 2, NOW.minusDays(2), NOW, SYMBOLS);

        when(cryptoRecsService.getStatisticsForTimeRange(any(), any(), any())).thenReturn(dummyStatistics);

        ResponseEntity<List<CryptoPriceStatistics>> responseEntity = cryptoRecsController
                .getStatisticsForTimeRange(Optional.empty(), NOW, NOW);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(SYMBOLS.length, responseEntity.getBody().size());
    }

    private static List<CryptoPrice> generateDummyData(Integer cout, Timestamp tp, String symbol, Double price) {
        List<CryptoPrice> cryptoPrices = new ArrayList<>(cout);
        IntStream.range(0, cout).forEach(i -> cryptoPrices.add(new CryptoPrice(tp, symbol, price)));
        return cryptoPrices;
    }

    private static List<CryptoPriceComputed> generateNormalized(Double price, String... symbols) {
        return Arrays.stream(symbols)
                .map(symbol -> new CryptoPriceComputed(symbol, price))
                .collect(Collectors.toList());
    }

    private static List<CryptoPriceStatistics> generateStatistics(
            Double max,
            Double min,
            LocalDateTime oldest,
            LocalDateTime newest,
            String... symbols) {
        return Arrays.stream(symbols)
                .map(symbol -> new CryptoPriceStatistics(symbol, oldest, newest, max, min))
                .collect(Collectors.toList());
    }
}
