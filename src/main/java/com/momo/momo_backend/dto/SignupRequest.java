package com.momo.momo_backend.dto;

import lombok.Getter;

@Getter
public class SignupRequest {
    private String id;
    private String password;
    private String nickname;
}
