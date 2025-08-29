package com.momo.momo_backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AiResultResponseDto {
    private String status;
    private ResultData result;

    @Getter
    @NoArgsConstructor
    public static class ResultData {
        private String summary;
        @JsonProperty("thumbnail_url")
        private String thumbnailUrl;
        private List<String> tags;
        private String title;
    }
}