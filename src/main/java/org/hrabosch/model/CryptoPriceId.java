package org.hrabosch.model;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class CryptoPriceId implements Serializable {

    private Timestamp timestamp;
    private String symbol;
}
