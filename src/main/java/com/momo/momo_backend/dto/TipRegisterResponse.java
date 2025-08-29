package com.momo.momo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TipRegisterResponse {
    private Long tipNo;
    private Long storageNo;
    private Boolean isPublic;
    private String summary;
    private String url;
    private Long userNo;
    private String nickname;
    private String thumbnailImageUrl;
    private String title;
    private List<String> tags;
}