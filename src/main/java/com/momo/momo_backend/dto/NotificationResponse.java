package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long id;
    private String type;              // 현재 엔티티는 String 타입
    private boolean isRead;
    private LocalDateTime createdAt;

    // 현 엔티티에는 tip 연관이 없으므로, 호환을 위해 필드는 유지하되 항상 null
    private Long tipId;               // nullable
    private String tipTitle;          // nullable

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getNo())
                .type(notification.getType())                 // ✅ .name() 제거 (String)
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .tipId(null)                                   // ✅ 엔티티에 tip 없음
                .tipTitle(null)                                // ✅ 엔티티에 tip 없음
                .build();
    }
}
