package org.hrabosch.repository;

import org.hrabosch.model.CryptoPrice;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.util.List;

public class CryptoStatsSpecification {

    public static Specification<CryptoPrice> notIn(List<String> disabledSymbols) {
        return (cryptoPriceRoot, cq, cb) -> cb.not(cryptoPriceRoot.get("symbol").in(disabledSymbols));
    }

    public static Specification<CryptoPrice> hasSymbol(String symbol) {
        return (cryptoPriceRoot, cq, cb) -> cb.equal(cryptoPriceRoot.get("symbol"), symbol);
    }

    public static Specification<CryptoPrice> isBetweenTimestamps(Timestamp from, Timestamp to) {
        return (cryptoPriceRoot, cq, cb) ->
            cb.between(cryptoPriceRoot.get("timestamp"), from, to);
    }

}
