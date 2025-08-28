package com.momo.momo_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "no")
    private Long no;

    /** 알림 대상 사용자 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_no", nullable = false)
    @JsonIgnore
    private User user;

    /** 알림 타입(예: tip:new, tip:update, comment:new ...) */
    @Column(name = "type", length = 50, nullable = false)
    private String type;

    /** 표시용 메시지 */
    @Column(name = "message", length = 1000)
    private String message;

    /** 클릭 시 이동할 경로(선택) */
    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    /** 읽음 여부 */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    /** 생성시각 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.read = true;
    }

    public static Notification of(User user, String type, String message, String linkUrl) {
        return Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .linkUrl(linkUrl)
                .read(false)
                .build();
    }
}
