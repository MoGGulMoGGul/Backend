package com.momo.momo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TipCreateResponse {
    private String url;
    private String title;
    private List<String> tags;
    private String summary;
    private String thumbnailImageUrl;
}