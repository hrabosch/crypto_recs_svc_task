package org.hrabosch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CryptoPriceComputed {
    private String symbol;
    private Double normalized;
}
