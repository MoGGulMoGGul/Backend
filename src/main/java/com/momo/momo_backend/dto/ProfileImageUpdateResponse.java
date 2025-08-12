package com.momo.momo_backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileImageUpdateResponse {
    private String profileImageUrl;
}