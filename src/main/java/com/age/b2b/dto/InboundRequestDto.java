package com.age.b2b.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboundRequestDto {

    @JsonProperty("prodCode")
    private String productCode;

    @JsonProperty("receivingQty")
    private int quantity;

    @JsonProperty("prodOrigin")
    private String origin;

    @JsonProperty("prodExpiry")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate expiryDate;

    @JsonProperty("receivingDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate inboundDate;

}