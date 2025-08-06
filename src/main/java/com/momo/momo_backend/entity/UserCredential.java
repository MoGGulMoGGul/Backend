package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_credentials")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserCredential {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @OneToOne(fetch = FetchType.LAZY)                // 🔄 ManyToOne → OneToOne
    @JoinColumn(name = "user_no", nullable = false)
    private User user;

    @Column(name = "id")
    private String loginId;                          // 필드명 ‘id’ 충돌 방지

    @Column(name = "pw", nullable = false, length = 255)
    private String pw;

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
