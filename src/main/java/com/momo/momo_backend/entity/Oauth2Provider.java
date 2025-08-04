package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "oauth2_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Oauth2Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no; // OAuth 제공자 식별 번호

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name; // 제공자 이름 (예: "Google", "Kakao")

    @Column(name = "activation", nullable = false)
    private Boolean activation = true; // 활성화 여부 (기본값: true)
}