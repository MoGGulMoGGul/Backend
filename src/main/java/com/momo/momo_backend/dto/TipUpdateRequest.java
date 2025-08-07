package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TipUpdateRequest {
    // 부분 수정 허용을 위해 모두 nullable 처리 (컨트롤러에서 @Valid는 형식 검증용)
    private String title;          // null이면 미수정
    private String contentSummary;        // null이면 미수정
    private Boolean isPublic;      // null이면 미수정
    private List<String> tags;     // null이면 미수정, []면 태그 모두 제거
}