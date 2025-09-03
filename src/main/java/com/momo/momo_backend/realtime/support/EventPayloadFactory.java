package com.momo.momo_backend.realtime.support;

import com.momo.momo_backend.realtime.RealtimeProperties;
import com.momo.momo_backend.realtime.dto.EventTypes;
import com.momo.momo_backend.realtime.dto.NotificationEvent;
import com.momo.momo_backend.realtime.dto.TipEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EventPayloadFactory {

    private final RealtimeProperties props;

    /** 최소 페이로드 (등록 직후, 제목/썸네일 미정 가능) */
    public TipEvent tipNewMinimal(Long tipId) {
        return TipEvent.builder()
                .type(EventTypes.TIP_NEW)
                .tipId(tipId)
                .createdAt(Instant.now())
                .v(props.getSchemaVersion())
                .build();
    }

    /** TipSummaryView 기반의 "신규" 풀 페이로드 */
    public TipEvent tipNewFromView(TipSummaryView v) {
        return TipEvent.builder()
                .type(EventTypes.TIP_NEW)
                .tipId(v.id())
                .title(v.title())
                .author(v.author())
                .tags(v.tags())
                .createdAt(v.createdAt())
                .thumbnailUrl(v.thumbnailUrl())
                .v(props.getSchemaVersion())
                .build();
    }

    /** TipSummaryView 기반의 "업데이트" 풀 페이로드 */
    public TipEvent tipUpdateFromView(TipSummaryView v) {
        return TipEvent.builder()
                .type(EventTypes.TIP_UPDATE)
                .tipId(v.id())
                .title(v.title())
                .author(v.author())
                .tags(v.tags())
                .createdAt(v.createdAt())
                .thumbnailUrl(v.thumbnailUrl())
                .v(props.getSchemaVersion())
                .build();
    }

    /** 새 알림 페이로드 (URL 명시) */
    public NotificationEvent notificationNew(Long tipId, String message, Instant createdAt, String url) {
        return NotificationEvent.builder()
                .type(EventTypes.NOTIFICATION_NEW)
                .tipId(tipId)
                .message(message)
                .createdAt(createdAt != null ? createdAt : Instant.now())
                .v(props.getSchemaVersion())
                .url(url)
                .build();
    }

    /** (호환용) 기존 시그니처 유지: tipId가 있으면 자동으로 "/tips/{id}" 링크 생성 */
    public NotificationEvent notificationNew(Long tipId, String message, Instant createdAt) {
        String autoUrl = (tipId != null) ? ("/tips/" + tipId) : null;
        return notificationNew(tipId, message, createdAt, autoUrl);
    }
}
