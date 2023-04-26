package org.hrabosch.model;


import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(CryptoPriceId.class)
public class CryptoPrice {
    @Id
    private Timestamp timestamp;
    @Id
    private String symbol;
    private Double price;
}
