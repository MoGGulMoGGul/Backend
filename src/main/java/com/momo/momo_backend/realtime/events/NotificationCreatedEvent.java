package com.momo.momo_backend.realtime.events;

import java.time.Instant;

/** 개인 알림 생성(팔로우/북마크 등)을 브로드캐스트하기 위한 이벤트 */
public record NotificationCreatedEvent(
        Long targetUserId,
        Long tipId,          // 없으면 null
        String message,
        Instant createdAt
) {}
