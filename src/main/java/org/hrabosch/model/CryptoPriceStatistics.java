package org.hrabosch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CryptoPriceStatistics {
    private String symbol;
    private LocalDateTime oldest;
    private LocalDateTime newest;
    private Double maxPrice;
    private Double minPrice;
}
