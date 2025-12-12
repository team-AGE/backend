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

    @Column(length = 16, unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ClientStatus approvalStatus;

    // 고객사 비고
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.note = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
