package com.momo.momo_backend.realtime.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

/** v1 스키마: 새 꿀팁 등록 시 피드로 브로드캐스트 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Jacksonized
@Builder
public record TipEvent(
        String type,          // EventTypes.TIP_NEW
        Long tipId,
        String title,
        String author,        // 작성자 닉네임(또는 표시명)
        List<String> tags,
        Instant createdAt,
        String thumbnailUrl,  // 썸네일(비동기 완료 전이면 null 가능)
        String v              // 스키마 버전("v1")
) {}
