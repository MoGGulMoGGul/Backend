package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter // Controller에서 JSON을 객체로 바인딩하기 위해 Setter가 필요합니다.
public class ProfileUpdateRequest {
    private String nickname;
}