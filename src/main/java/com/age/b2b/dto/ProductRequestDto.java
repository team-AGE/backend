package com.age.b2b.dto;

import com.age.b2b.domain.common.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRegisterDto {
    private String productCode;
    private String name;
    private int consumerPrice;
    private int supplyPrice;
    private int costPrice;
    private String origin;
    private String description;
    private ProductStatus status;
}
