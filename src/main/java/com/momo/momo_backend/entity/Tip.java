package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Tip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no")
    private User user;

    private String url;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String contentSummary;
    private String thumbnailUrl;
    private Boolean isPublic;

    // AI 작업 ID를 저장할 필드 추가
    private String taskId;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bookmark> bookmarks = new ArrayList<>();

    @OneToMany(mappedBy = "tip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorageTip> storageTips = new ArrayList<>();

    @OneToMany(mappedBy = "tip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "tip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TipTag> tipTags = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public Tip(User user, String url, String title, String contentSummary, String thumbnailUrl, Boolean isPublic, String taskId) {
        this.user = user;
        this.url = url;
        this.title = title;
        this.contentSummary = contentSummary;
        this.thumbnailUrl = thumbnailUrl;
        this.isPublic = isPublic;
        this.taskId = taskId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
