package org.hrabosch.repository;

import org.hrabosch.model.CryptoPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface CryptoPriceRepository  extends JpaRepository<CryptoPrice, Long>, JpaSpecificationExecutor<CryptoPrice> {

    @Query("SELECT symbol FROM CryptoPrice GROUP BY symbol")
    List<String> findGroupBySymbolWithNativeQuery();

    Optional<CryptoPrice> findFirstBySymbolOrderByPriceDesc(String symbol);

    Optional<CryptoPrice> findFirstBySymbolOrderByPriceAsc(String symbol);

    @Query("SELECT p FROM CryptoPrice p "
            + "WHERE p.symbol = :symbol "
            + "AND p.timestamp >= :from AND p.timestamp <= :to")
    List<CryptoPrice> findAllBySymbolAndTimestamp(
            @Param("symbol") String symbol,
            @Param("from") Timestamp from,
            @Param("to") Timestamp to);
}
