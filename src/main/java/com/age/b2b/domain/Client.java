package com.age.b2b.domain;

import com.age.b2b.domain.common.ClientStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_id")
    private Long clientId;

    @Column(length = 20, nullable = false)
    private String clientCategory;

    @Column(length = 50, nullable = false)
    private String businessName;

    @Column(length = 20, unique = true, nullable = false)
    private String businessNumber;

    @Column(length = 50, nullable = false)
    private String ownerName;

    @Column(length = 20, unique = true, nullable = false)
    private String phone;

    @Column(length = 100, unique = true, nullable = false)
    private String email;

    @Column(length = 50, unique = true, nullable = false) // length 조정 (16 -> 50)
    private String username;

    @Column(nullable = false)
    private String password;

    // --- [수정] 주소 필드 세분화 ---
    @Column(length = 10)
    private String zipCode;       // 우편번호 (추가)

    @Column(nullable = false)
    private String address;       // 기본 주소

    @Column(nullable = false)
    private String detailAddress; // 상세 주소 (추가)
    // ---------------------------

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ClientStatus approvalStatus;

    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 사업자등록증 이미지 경로 저장용
    @Column(nullable = false)
    private String businessLicensePath;

    @PrePersist
    public void prePersist() {
        this.note = null;
        this.approvalStatus = ClientStatus.WAITING; // 기본값 설정
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}