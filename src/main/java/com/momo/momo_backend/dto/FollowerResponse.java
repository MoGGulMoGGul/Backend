package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowerResponse {
    private Long userNo;
    private String nickname;
    private String profileImageUrl;

    public static FollowerResponse from(User user) {
        return FollowerResponse.builder()
                .userNo(user.getNo())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImage())
                .build();
    }
}