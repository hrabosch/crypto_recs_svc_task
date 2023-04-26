package org.hrabosch.service;

import lombok.extern.slf4j.Slf4j;
import org.hrabosch.model.CryptoPrice;
import org.hrabosch.model.CryptoPriceComputed;
import org.hrabosch.model.CryptoPriceStatistics;
import org.hrabosch.repository.CryptoPriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.hrabosch.repository.CryptoStatsSpecification.hasSymbol;
import static org.hrabosch.repository.CryptoStatsSpecification.isBetweenTimestamps;
import static org.hrabosch.repository.CryptoStatsSpecification.notIn;

@Service
@Slf4j
public class CryptoRecsService {

    @Value("${disableSymbols:}#{T(java.util.Collections).emptyList()}")
    private List<String> disabledSymbols;

    private CryptoPriceRepository cryptoPriceRepository;

    @Autowired
    public CryptoRecsService(CryptoPriceRepository cryptoPriceRepository) {
        this.cryptoPriceRepository = cryptoPriceRepository;
    }

    public List<CryptoPrice> findAll() {
        return cryptoPriceRepository.findAll(notIn(disabledSymbols));
    }

    public List<CryptoPrice> findBySymbol(String symbol) {
        return cryptoPriceRepository.findAll(notIn(disabledSymbols).and(hasSymbol(symbol)));
    }

    public List<CryptoPriceComputed> getAllNormalized(Sort.Direction sort) {
        List<String> storedSymbols = getAllowedSymbols();
        log.debug("Computing stats for following symbols: {}", storedSymbols);
        List<CryptoPriceComputed> computedList = storedSymbols.stream()
                .map(symbol -> {
                    Double norm = computeNormalizedPriceForSymbol(symbol);
                    return new CryptoPriceComputed(symbol, norm);
                })
                .collect(Collectors.toList());

        computedList.sort((CryptoPriceComputed c1, CryptoPriceComputed c2) -> {
            if (sort.isAscending()) {
                return c1.getNormalized().compareTo(c2.getNormalized());
            } else {
                return c2.getNormalized().compareTo(c1.getNormalized());
            }
        });
        return computedList;
    }

    private List<String> getAllowedSymbols() {
        List<String> storedSymbols = cryptoPriceRepository.findGroupBySymbolWithNativeQuery();
        if (disabledSymbols != null && !disabledSymbols.isEmpty()) {
            storedSymbols.removeAll(disabledSymbols);
        }
        return storedSymbols;
    }

    private Double computeNormalizedPriceForSymbol(String symbol) throws ArithmeticException {
        Double min = cryptoPriceRepository.findFirstBySymbolOrderByPriceAsc(symbol).get().getPrice();
        Double max = cryptoPriceRepository.findFirstBySymbolOrderByPriceDesc(symbol).get().getPrice();
        log.debug("Found min: {}, max:{} for symbol: {}", min, max, symbol);
        return normalize(min, max);
    }

    public List<CryptoPriceStatistics> getStatistics(Optional<String> symbol, Optional<LocalDate> yearMonth) {
        return symbol.isPresent()
                ? List.of(getMonthStatisticsForSymbol(symbol.get(), yearMonth))
                : getStatisticsForAllSymbols(yearMonth);
    }

    private List<CryptoPriceStatistics> getStatisticsForAllSymbols(Optional<LocalDate> yearMonth) {
        List<String> symbols = getAllowedSymbols();
        List<CryptoPriceStatistics> statistics = symbols.stream()
                .map(symbol -> getMonthStatisticsForSymbol(symbol, yearMonth))
                .collect(Collectors.toList());
        return statistics;
    }

    private CryptoPriceStatistics getMonthStatisticsForSymbol(String symbol, Optional<LocalDate> yearMonth) {
        List<CryptoPrice> cryptoPrices;
        if (yearMonth.isPresent()) {
            Timestamp from = Timestamp.valueOf(yearMonth.get().with(firstDayOfMonth()).atStartOfDay());
            Timestamp to = Timestamp.valueOf(yearMonth.get().with(lastDayOfMonth()).atStartOfDay());
            cryptoPrices = cryptoPriceRepository.findAll(
                    hasSymbol(symbol).and(isBetweenTimestamps(from, to)));
        } else {
            cryptoPrices = cryptoPriceRepository.findAll(hasSymbol(symbol));
        }
        return findStatistics(cryptoPrices, symbol);
    }

    private CryptoPriceStatistics getStatistics(String symbol, LocalDateTime from, LocalDateTime to) {
        List<CryptoPrice> cryptoPrices = cryptoPriceRepository.findAll(hasSymbol(symbol)
                .and(isBetweenTimestamps(Timestamp.valueOf(from), Timestamp.valueOf(to))));
        return findStatistics(cryptoPrices, symbol);
    }


    private CryptoPriceStatistics findStatistics(List<CryptoPrice> cryptoPrices, String symbol) {
        CryptoPriceStatistics statistics = new CryptoPriceStatistics();
        statistics.setSymbol(symbol);
        if (cryptoPrices.isEmpty()) {
            return statistics;
        }
        DoubleSummaryStatistics priceStatistics = cryptoPrices.stream()
                .mapToDouble(CryptoPrice::getPrice)
                .summaryStatistics();
        LongSummaryStatistics timestampStatistics = cryptoPrices.stream()
                .mapToLong(cp -> cp.getTimestamp().getTime())
                .summaryStatistics();

        statistics.setOldest(Instant.ofEpochMilli(timestampStatistics.getMin())
                .atZone(ZoneId.systemDefault()).toLocalDateTime());
        statistics.setNewest(Instant.ofEpochMilli(timestampStatistics.getMax())
                .atZone(ZoneId.systemDefault()).toLocalDateTime());
        statistics.setMaxPrice(priceStatistics.getMax());
        statistics.setMinPrice(priceStatistics.getMin());

        return statistics;
    }

    public List<CryptoPriceStatistics> getStatisticsForTimeRange(Optional<String> symbol, LocalDateTime from, LocalDateTime to) {
        if (symbol.isPresent()) {
            return List.of(getStatistics(symbol.get(), from, to));
        }
        List<String> symbols = getAllowedSymbols();
        List<CryptoPriceStatistics> statistics = symbols.stream()
                .map(s -> getStatistics(s, from, to))
                .collect(Collectors.toList());
        return statistics;
    }

    public Optional<CryptoPriceComputed> getHighestNormalize(LocalDate date) {
        List<String> allowed = getAllowedSymbols();
        Timestamp from = Timestamp.valueOf(date.atTime(LocalTime.MIN));
        Timestamp to = Timestamp.valueOf(date.atTime(LocalTime.MAX));

        Optional<CryptoPriceComputed> cryptoPriceComputed = allowed.stream()
                .map(symbol -> computeNormalizedForTimeRange(symbol, from, to))
                .collect(Collectors.toList())
                .stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(CryptoPriceComputed::getNormalized));
        return cryptoPriceComputed;
    }

    private CryptoPriceComputed computeNormalizedForTimeRange(String symbol, Timestamp from, Timestamp to)
            throws ArithmeticException {
        List<CryptoPrice> cryptoPrices = cryptoPriceRepository.findAllBySymbolAndTimestamp(symbol, from, to);
        if (cryptoPrices.isEmpty()) {
            return null;
        }
        DoubleSummaryStatistics priceStats = cryptoPrices.stream()
                .mapToDouble(CryptoPrice::getPrice)
                .summaryStatistics();
        Double max = priceStats.getMax();
        Double min = priceStats.getMin();
        return new CryptoPriceComputed(symbol, normalize(min, max));
    }

    public static double normalize(double min, double max) {
        return (max - min) / min;
    }
}
