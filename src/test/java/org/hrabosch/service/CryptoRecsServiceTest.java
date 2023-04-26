package org.hrabosch.service;

import org.hrabosch.model.CryptoPrice;
import org.hrabosch.model.CryptoPriceComputed;
import org.hrabosch.model.CryptoPriceStatistics;
import org.hrabosch.repository.CryptoPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CryptoRecsServiceTest {

    @Mock
    private CryptoPriceRepository cryptoPriceRepository;
    private CryptoRecsService cryptoRecsService;

    public static final LocalDate NOW = LocalDate.now();
    public static final String[] SYMBOLS = {"AAA", "BBB", "CCC"};
    public static final Double PRICE = 1.123456;
    public static final Integer COUNT = 10;

    @BeforeEach
    void initData() {
        this.cryptoRecsService = new CryptoRecsService(cryptoPriceRepository);
    }

    @Test
    void testHighestNormalizedValueWhenNoDataAvailable() {
        when(cryptoPriceRepository.findGroupBySymbolWithNativeQuery()).thenReturn(Arrays.asList("ABC", "DEF"));
        when(cryptoPriceRepository.findAllBySymbolAndTimestamp(anyString(), any(), any()))
                .thenReturn(Collections.emptyList());
        assertFalse(cryptoRecsService.getHighestNormalize(NOW).isPresent());
    }

    @Test
    void testHighestNormalizedValueWhenDataAvailable() {
        Timestamp ts = Timestamp.valueOf(NOW.atStartOfDay());
        List<CryptoPrice> cryptoPriceList = Arrays.asList(
                new CryptoPrice(ts, SYMBOLS[0], PRICE),
                new CryptoPrice(ts, SYMBOLS[0], PRICE));

        when(cryptoPriceRepository.findGroupBySymbolWithNativeQuery()).thenReturn(Arrays.asList(SYMBOLS));
        when(cryptoPriceRepository.findAllBySymbolAndTimestamp(eq(SYMBOLS[1]), any(), any()))
                .thenReturn(Arrays.asList(new CryptoPrice(ts, SYMBOLS[1], PRICE)));
        when(cryptoPriceRepository.findAllBySymbolAndTimestamp(eq(SYMBOLS[0]), any(), any()))
                .thenReturn(cryptoPriceList);

        assertTrue(cryptoRecsService.getHighestNormalize(NOW).isPresent());
    }

    @Test
    void testComputeStatisticsForAllData() {
        Timestamp ts = Timestamp.valueOf(NOW.atStartOfDay());
        List<CryptoPrice> cryptoPriceList = Arrays.asList(
                new CryptoPrice(ts, SYMBOLS[0], PRICE),
                new CryptoPrice(ts, SYMBOLS[0], PRICE));

        when(cryptoPriceRepository.findGroupBySymbolWithNativeQuery()).thenReturn(Arrays.asList(SYMBOLS[0]));
        when(cryptoPriceRepository.findAll(any(Specification.class))).thenReturn(cryptoPriceList);

        List<CryptoPriceStatistics> result = cryptoRecsService.getStatistics(
                Optional.empty(),
                Optional.empty());
        assertFalse(result.isEmpty());
        assertEquals(result.get(0).getMaxPrice(), PRICE);
        assertEquals(result.get(0).getMinPrice(), PRICE);
    }

    @Test
    void testComputeStatisticsForGivenSymbol() {
        Timestamp ts = Timestamp.valueOf(NOW.atStartOfDay());
        List<CryptoPrice> dummy = generateDummyData(COUNT, ts, SYMBOLS[0], PRICE);
        dummy.addAll(generateDummyData(COUNT - (COUNT - 1), ts, SYMBOLS[0], PRICE / 2));
        dummy.addAll(generateDummyData(COUNT - (COUNT - 1), ts, SYMBOLS[0], PRICE * 2));

        when(cryptoPriceRepository.findAll(any(Specification.class))).thenReturn(dummy);

        List<CryptoPriceStatistics> result = cryptoRecsService.getStatistics(
                Optional.of(SYMBOLS[0]),
                Optional.of(NOW));
        assertFalse(result.isEmpty());
        assertEquals(result.get(0).getMaxPrice(), PRICE * 2);
        assertEquals(result.get(0).getMinPrice(), PRICE / 2);
    }

    @Test
    void testStatisticsForAllInDefinedRange() {
        LocalDateTime now = LocalDateTime.now();
        Timestamp ts = Timestamp.valueOf(now);
        List<CryptoPrice> dummy = generateDummyDataWithMinMax(COUNT, ts, SYMBOLS[0], PRICE);

        when(cryptoPriceRepository.findGroupBySymbolWithNativeQuery()).thenReturn(Collections.singletonList(SYMBOLS[0]));
        when(cryptoPriceRepository.findAll(any(Specification.class))).thenReturn(dummy);

        List<CryptoPriceStatistics> result = cryptoRecsService
                .getStatisticsForTimeRange(Optional.empty(), now, now);

        assertEquals(1, result.size());
        CryptoPriceStatistics stats = result.get(0);
        assertTrue(stats.getMaxPrice() >stats.getMinPrice());
        assertEquals(SYMBOLS[0], stats.getSymbol());
    }

    @Test
    void testFindAllCryptos() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        List<CryptoPrice> dummy = generateDummyData(COUNT, now, SYMBOLS[0], PRICE);
        dummy.addAll(generateDummyData(COUNT / 2, now, SYMBOLS[1], PRICE / 2));

        when(cryptoPriceRepository.findAll(any(Specification.class)))
                .thenReturn(dummy);

        List<CryptoPrice> results = cryptoRecsService.findAll();

        assertFalse(results.isEmpty());
        assertEquals(COUNT + COUNT / 2, results.size());
    }

    @Test
    void testFindBySymbol() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        List<CryptoPrice> dummy = generateDummyData(COUNT, now, SYMBOLS[0], PRICE);
        when(cryptoPriceRepository.findAll(any(Specification.class)))
                .thenReturn(dummy);

        List<CryptoPrice> results = cryptoRecsService.findBySymbol(SYMBOLS[0]);

        assertFalse(results.isEmpty());
        assertEquals(COUNT, results.size());
    }

    @Test
    void testGetAllNormalizedAscOrder() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        when(cryptoPriceRepository.findGroupBySymbolWithNativeQuery()).thenReturn(Arrays.stream(SYMBOLS).toList());

        when(cryptoPriceRepository.findFirstBySymbolOrderByPriceAsc(anyString()))
                .thenReturn(Optional.of(new CryptoPrice(now, SYMBOLS[0], PRICE / 2)));
        when(cryptoPriceRepository.findFirstBySymbolOrderByPriceDesc(anyString()))
                .thenReturn(Optional.of(new CryptoPrice(now, SYMBOLS[0], PRICE)));

        when(cryptoPriceRepository.findFirstBySymbolOrderByPriceAsc(eq(SYMBOLS[1])))
                .thenReturn(Optional.of(new CryptoPrice(now, SYMBOLS[0], PRICE / 3)));
        when(cryptoPriceRepository.findFirstBySymbolOrderByPriceDesc(eq(SYMBOLS[1])))
                .thenReturn(Optional.of(new CryptoPrice(now, SYMBOLS[0], PRICE * 2)));

        List<CryptoPriceComputed> results = cryptoRecsService.getAllNormalized(Sort.Direction.ASC);

        assertFalse(results.isEmpty());
        assertEquals(SYMBOLS.length, results.size());
        assertTrue(results.get(0).getNormalized() < results.get(results.size() - 1).getNormalized());

    }

    @Test
    void testGetAllNormalizedDescOrder() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        when(cryptoPriceRepository.findGroupBySymbolWithNativeQuery()).thenReturn(Arrays.stream(SYMBOLS).toList());

        when(cryptoPriceRepository.findFirstBySymbolOrderByPriceAsc(anyString()))
                .thenReturn(Optional.of(new CryptoPrice(now, SYMBOLS[0], PRICE / 2)));
        when(cryptoPriceRepository.findFirstBySymbolOrderByPriceDesc(anyString()))
                .thenReturn(Optional.of(new CryptoPrice(now, SYMBOLS[0], PRICE)));

        when(cryptoPriceRepository.findFirstBySymbolOrderByPriceAsc(eq(SYMBOLS[1])))
                .thenReturn(Optional.of(new CryptoPrice(now, SYMBOLS[0], PRICE / 3)));
        when(cryptoPriceRepository.findFirstBySymbolOrderByPriceDesc(eq(SYMBOLS[1])))
                .thenReturn(Optional.of(new CryptoPrice(now, SYMBOLS[0], PRICE * 2)));

        List<CryptoPriceComputed> results = cryptoRecsService.getAllNormalized(Sort.Direction.DESC);

        assertFalse(results.isEmpty());
        assertEquals(SYMBOLS.length, results.size());
        assertTrue(results.get(0).getNormalized() > results.get(results.size() - 1).getNormalized());

    }

    private static List<CryptoPrice> generateDummyData(Integer count, Timestamp tp, String symbol, Double price) {
        List<CryptoPrice> cryptoPrices = new ArrayList<>(count);
        IntStream.range(0, count).forEach(i -> cryptoPrices.add(new CryptoPrice(tp, symbol, price)));
        return cryptoPrices;
    }

    private static List<CryptoPrice> generateDummyDataWithMinMax(
            Integer count, Timestamp tp, String symbol, Double price) {
        List<CryptoPrice> prices = generateDummyData(count, tp, symbol, price);
        prices.addAll(generateDummyData(1, tp, symbol, price / 2));
        prices.addAll(generateDummyData(1, tp, symbol, price * 2));
        return prices;
    }
}
