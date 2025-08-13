package com.momo.momo_backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileUpdateResponse {
    private String message;
    private String profileImageUrl;
}