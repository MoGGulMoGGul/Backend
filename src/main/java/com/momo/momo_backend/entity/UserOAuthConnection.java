package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_oauth_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOAuthConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no; // OAuth 연결 식별 번호

    @Column(name = "oauth2_user_id", nullable = false, length = 255)
    private String oauth2UserId; // OAuth 제공자에서 발급한 사용자 ID

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 연결 생성 시각

    @Column(name = "provider_no", nullable = false)
    private Long providerNo; // 연결된 OAuth 제공자 번호 (oauth2_providers.no)

    @Column(name = "user_no", nullable = false)
    private Long userNo; // 내부 사용자 번호 (users.no)
}
