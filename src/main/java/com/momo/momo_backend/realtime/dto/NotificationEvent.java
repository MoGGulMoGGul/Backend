package com.momo.momo_backend.realtime.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/** v1 스키마: 팔로우/북마크/그룹보관함 등록 등 개인 큐 알림 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Jacksonized
@Builder
public record NotificationEvent(
        String type,        // EventTypes.NOTIFICATION_NEW
        Long tipId,         // 해당되면 포함 (없으면 null)
        String message,     // 사용자에게 보여줄 간단 메시지
        Instant createdAt,
        String v,           // "v1"
        String url          // 상세로 이동할 URL (tipId 있으면 "/tips/{id}")
) {}
