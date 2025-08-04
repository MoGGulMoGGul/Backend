package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no; // 사용자 자격 정보 식별 번호

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false)
    private User user; // 연결된 사용자 번호 (users.no)

    @Column(name = "id", nullable = false, length = 50)
    private String id; // 사용자 로그인 ID

    @Column(name = "pw", nullable = false, length = 255)
    private String pw; // 암호화된 비밀번호

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now(); // 마지막 업데이트 시각
}