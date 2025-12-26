package com.age.b2b.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryInfo {
    private String receiverName;  // 수취인
    private String receiverPhone; // 연락처
    private String zipCode;         // 우편번호
    private String address;       // 배송지 주소
    private String detailAddress;   // 상세주소
    private String memo;          // 배송 메모
}
