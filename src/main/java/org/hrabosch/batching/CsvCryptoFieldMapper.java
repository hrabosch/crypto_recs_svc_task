package org.hrabosch.batching;

import org.hrabosch.model.CryptoPrice;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;

import java.sql.Timestamp;

@Component
public class CsvCryptoFieldMapper implements FieldSetMapper<CryptoPrice> {
    @Value("#{T(java.util.Arrays).asList('${input.headers}')}")
    private String[] inputHeaders;
    @Override
    public CryptoPrice mapFieldSet(FieldSet fieldSet) throws BindException {
        return new CryptoPrice(new Timestamp(
                fieldSet.readLong(inputHeaders[0])),
                fieldSet.readString(inputHeaders[1]),
                fieldSet.readDouble(inputHeaders[2])
        );
    }
}
