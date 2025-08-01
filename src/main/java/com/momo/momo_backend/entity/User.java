package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @Column(length = 50, nullable = false, unique = true)
    private String id;

    @Column(length = 255, nullable = false)
    private String password;

    @Column(length = 50, nullable = false, unique = true)
    private String nickname;

    @Column(name = "profile_image", columnDefinition = "TEXT")
    private String profileImage = "default_profile.png";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ========== 관계 설정 ==========

    // 1:1 관계 (User ↔ UserCredential)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private UserCredential credential;

    // 1:N 관계 (User ↔ UserOAuthConnection)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserOAuthConnection> oauthConnections = new ArrayList<>();

    // 1:N 관계 (User ↔ GroupMember)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> groupMembers = new ArrayList<>();

    // 1:N 관계 (User ↔ Storage)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Storage> storages = new ArrayList<>();

    // 1:N 관계 (User ↔ Notification)
    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    // 1:N 관계 (User ↔ Tip)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tip> tips = new ArrayList<>();

    // 1:N 관계 (User ↔ Bookmark)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bookmark> bookmarks = new ArrayList<>();

    // Getter / Setter 생략 가능 (Lombok 사용 시 @Getter @Setter 또는 @Data로 처리)

    // 생성자, equals/hashCode, toString 생략
}

