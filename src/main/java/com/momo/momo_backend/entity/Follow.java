package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "follow")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no; // 팔로우 식별 번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_no", nullable = false)
    private User follower; // 팔로우를 건 사람

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_no", nullable = false)
    private User following; // 팔로우 당한 사람

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 팔로우 생성 시간
}