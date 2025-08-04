package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;  // 그룹 식별 번호 (PK)

    @Column(nullable = false, length = 100)
    private String name;  // 그룹명

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();  // 생성일

    // ========== 연관 관계 ==========

    // 1:N 관계 - GroupMember
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> groupMembers = new ArrayList<>();

    // 1:N 관계 - Storage
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Storage> storages = new ArrayList<>();

    // 생성자, getter/setter 생략 가능 (Lombok 사용 가능)
}
