package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long id;
    private String type;              // FOLLOWED_ME, BOOKMARKED_MY_TIP 등
    private boolean isRead;
    private LocalDateTime createdAt;
    private Long tipId;              // 관련 꿀팁 ID (nullable)
    private String tipTitle;         // 관련 꿀팁 제목 (nullable)

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getNo())
                .type(notification.getType().name())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .tipId(notification.getTip() != null ? notification.getTip().getNo() : null)
                .tipTitle(notification.getTip() != null ? notification.getTip().getTitle() : null)
                .build();
    }
}
