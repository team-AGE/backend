package com.age.b2b.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter // 1. 데이터를 채워 넣기 위해 Setter 추가
@Builder
@NoArgsConstructor  // 2. JSON 파싱을 위한 기본 생성자 필수
@AllArgsConstructor // 3. Builder 사용을 위한 전체 생성자 필수
public class InboundRequestDto {

    // [추가] 화면에서 가장 먼저 입력하는 '상품코드'
    @JsonProperty("prodCode")
    private String productCode;

    // [기존 유지] 입고 수량
    @JsonProperty("receivingQty")
    private int quantity;

    // [추가] 리액트 화면 하단에 있는 '원산지' 입력 필드
    @JsonProperty("prodOrigin")
    private String origin;

    // [날짜 포맷 지정] 화면의 YY/MM/DD 형식을 백엔드 LocalDate로 안전하게 변환
    @JsonProperty("prodExpiry")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate expiryDate; // 유통기한

    @JsonProperty("receivingDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate inboundDate;// 입고일자

    // [참고] 화면에서 "자동 생성"이라 했으므로, 프론트에서 안 보내도 되지만
    // 나중에 입고 수정 시 사용할 수 있으므로 남겨둡니다.
    private String lotNumber;
}