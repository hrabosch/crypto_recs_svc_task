package org.hrabosch.batching;

import lombok.extern.slf4j.Slf4j;
import org.hrabosch.model.CryptoPrice;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public class CryptoPriceItemProcessor implements ItemProcessor<CryptoPrice, CryptoPrice> {

    @Override
    public CryptoPrice process(CryptoPrice item) throws Exception {
        CryptoPrice cryptoPrice = new CryptoPrice(item.getTimestamp(), item.getSymbol(), item.getPrice());
        log.debug("Processed crypto item line: {}", cryptoPrice);
        return cryptoPrice;
    }
}
