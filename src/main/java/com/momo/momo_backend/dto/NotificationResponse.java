package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long id;
    private String type;          // 응답은 String
    private boolean isRead;
    private LocalDateTime createdAt;

    private Long tipId;           // nullable
    private String tipTitle;      // nullable

    public static NotificationResponse fromEntity(Notification n) {
        return NotificationResponse.builder()
                .id(n.getNo())
                .type(n.getType().name())              // ✅ enum → String
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .tipId(n.getTip() != null ? n.getTip().getNo() : null)
                .tipTitle(n.getTip() != null ? n.getTip().getTitle() : null)
                .build();
    }
}
