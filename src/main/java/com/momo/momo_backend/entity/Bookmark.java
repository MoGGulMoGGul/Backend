package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookmark")
@Getter
@Setter
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;  // 즐겨찾기 식별 번호

    // N(o):1 관계 - 사용자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_no", nullable = false)
    private User user;

    // N(o):1 관계 - 꿀팁
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tip_no", nullable = false)
    private Tip tip;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();  // 즐겨찾기 등록일

    // 생성자, getter/setter 등은 Lombok 사용 가능
}

