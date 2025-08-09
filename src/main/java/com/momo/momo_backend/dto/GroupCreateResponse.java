package com.momo.momo_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor; // AllArgsConstructor 추가

@Getter
@Builder
@AllArgsConstructor // Builder 패턴 사용을 위해 AllArgsConstructor 추가
public class GroupCreateResponse {
    private String message;  // 성공 메시지
    private Long groupNo;    // 생성된 그룹의 식별 번호
}