package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.Tip;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipResponse {
    private Long id;
    private String title;
    private String contentSummary;
    private String url;
    private String nickname;
    private LocalDateTime createdAt;

    public static TipResponse fromEntity(Tip tip) {
        return TipResponse.builder()
                .id(tip.getNo())
                .title(tip.getTitle())
                .contentSummary(tip.getContentSummary())
                .url(tip.getUrl())
                .nickname(tip.getUser().getNickname())
                .createdAt(tip.getCreatedAt())
                .build();
    }
}
