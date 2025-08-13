package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tip")
public class Tip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;  // 꿀팁 식별 번호

    // 작성자 (N(o):1 관계)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_no", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String url;  // 저장할 URL (nullable)

    @Column(length = 255)
    private String title;  // 꿀팁 제목

    @Column(name = "content_summary", columnDefinition = "TEXT")
    private String contentSummary;  // 요약

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl; // 썸네일 URL 필드

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;  // 공개 여부

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ========== 연관 관계 ==========

    // 1:N(o) - StorageTip
    @Builder.Default
    @OneToMany(mappedBy = "tip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorageTip> storageTips = new ArrayList<>();

    // 1:N(o) - Notification
    @Builder.Default
    @OneToMany(mappedBy = "tip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    // 1:N(o) - TipTag
    @Builder.Default
    @OneToMany(mappedBy = "tip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TipTag> tipTags = new ArrayList<>();

    // 1:N(o) - Bookmark
    @Builder.Default
    @OneToMany(mappedBy = "tip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bookmark> bookmarks = new ArrayList<>();
}
