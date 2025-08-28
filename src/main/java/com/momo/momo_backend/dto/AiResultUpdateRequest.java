package com.momo.momo_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiResultUpdateRequest {
    private Long tipNo;
    private String title;
    private String contentSummary;
    private List<String> tags;
    private String thumbnailUrl;
}