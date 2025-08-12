package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.Tip;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WeeklyRankingResponse {
    private Long tipId;
    private String title;
    private String thumbnailUrl;
    private String nickname;
    private List<String> tags;
    private Long weeklyBookmarkCount;

    // Tip 엔티티와 즐겨찾기 수를 받아 DTO를 생성하는 정적 팩토리 메서드
    public static WeeklyRankingResponse from(Tip tip, Long weeklyBookmarkCount) {
        return WeeklyRankingResponse.builder()
                .tipId(tip.getNo())
                .title(tip.getTitle())
                .thumbnailUrl(tip.getThumbnailUrl())
                .nickname(tip.getUser().getNickname())
                .tags(TipResponse.from(tip).getTags()) // 기존 TipResponse의 로직 재활용
                .weeklyBookmarkCount(weeklyBookmarkCount)
                .build();
    }
}