package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.Tag;
import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.entity.TipTag;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Getter
@Builder
public class TipResponse {
    private Long no;
    private String title;
    private String content;
    private Boolean isPublic;
    private java.util.List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TipResponse from(Tip tip) {
        return TipResponse.builder()
                .no(tip.getNo())
                .title(tip.getTitle())
                .content(tip.getContentSummary())
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