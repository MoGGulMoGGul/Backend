package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnfollowRequest {
    private String followeeId; // 팔로우를 취소할 대상의 로그인 아이디
}