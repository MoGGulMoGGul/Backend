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

    @Column(name = "follower_no", nullable = false)
    private Long followerNo; // 팔로우 요청한 사용자 ID (users.no)

    @Column(name = "following_no", nullable = false)
    private Long followingNo; // 팔로우 당한 사용자 ID (users.no)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 팔로우 생성 시간
}