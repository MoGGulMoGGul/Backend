package com.momo.momo_backend.realtime.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Jacksonized
@Builder
public record TipViewsRankEvent(
        String type,                // "tip:views:rank:update"
        List<Item> leaderboard,     // 상위 N
        String v                    // "v1"
) {
    @Jacksonized @Builder
    public record Item(Long tipId, String title, Double score) {}
}
