package com.momo.momo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인/토큰 갱신 응답 DTO
 * - login 성공 시: accessToken, refreshToken, userNo 세 필드 모두 채워서 반환
 * - refresh 시: 기존 컨트롤러 호환을 위해 (accessToken, refreshToken) 2-인자 생성자도 지원
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long userNo; // 로그인 성공 시 프론트가 필요로 하는 값

    /** 기존 컨트롤러(refresh) 코드 호환용: userNo 없이 반환해도 되도록 유지 */
    public LoginResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userNo = null;
    }
}
