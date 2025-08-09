package com.momo.momo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;

@Getter
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private int status;
    private String message;
    private String error; // 예외 타입 (선택 사항, 디버깅에 유용)
}
