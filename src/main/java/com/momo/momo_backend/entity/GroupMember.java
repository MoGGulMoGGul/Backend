package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_member")
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;  // 그룹 멤버 고유 ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_no", nullable = false)
    private User user;  // 소속 사용자

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_no", nullable = false)
    private Group group;  // 소속 그룹

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();  // 가입일

    // 기본 생성자, getter/setter 생략 가능 (Lombok 사용 시)
}
