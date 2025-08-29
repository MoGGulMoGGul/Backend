package com.momo.momo_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class TipRegisterRequest {

    @NotEmpty
    private String url;

    @NotEmpty
    private String title;

    private String summary;

    private String thumbnailImageUrl;

    private List<String> tags;

    @NotEmpty
    private Long storageNo;

    @NotEmpty
    private Boolean isPublic;
}