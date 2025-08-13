package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FollowRequest {
    private String followeeId; // 팔로우할 대상의 로그인 아이디
}