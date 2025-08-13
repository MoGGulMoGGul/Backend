package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.Tag;
import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.entity.TipTag;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class TipResponse {
    private Long no;                    // 꿀팁 ID
    private String title;              // 꿀팁 제목
    private String contentSummary;     // 꿀팁 요약
    private String url;                // 원본 URL
    private Long userNo;               // 작성자 No
    private String nickname;           // 작성자 닉네임
    private String thumbnailUrl;       // 썸네일 URL
    private Boolean isPublic;          // 공개 여부
    private List<String> tags;         // 태그 리스트
    private LocalDateTime createdAt;   // 생성일
    private LocalDateTime updatedAt;   // 수정일

    public static TipResponse from(Tip tip) {
        return TipResponse.builder()
                .no(tip.getNo())
                .title(tip.getTitle())
                .contentSummary(tip.getContentSummary())
                .url(tip.getUrl())
                .userNo(tip.getUser().getNo())
                .nickname(tip.getUser().getNickname())
                .thumbnailUrl(tip.getThumbnailUrl())
                .isPublic(tip.getIsPublic())
                .tags(tip.getTipTags().stream()
                        .map(TipTag::getTag)
                        .map(Tag::getName)
                        .collect(Collectors.toList()))
                .createdAt(tip.getCreatedAt())
                .updatedAt(tip.getUpdatedAt())
                .build();
    }
}
