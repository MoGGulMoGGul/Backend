package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_oauth_connections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserOAuthConnection {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @Column(name = "oauth2_user_id", nullable = false, length = 255)
    private String oauth2UserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)          // ★ 추가
    @JoinColumn(name = "provider_no", nullable = false)
    private Oauth2Provider provider;                              // FK → 객체

    @ManyToOne(fetch = FetchType.LAZY, optional = false)          // ★ 핵심
    @JoinColumn(name = "user_no", nullable = false)
    private User user;                                            // FK → 객체

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
