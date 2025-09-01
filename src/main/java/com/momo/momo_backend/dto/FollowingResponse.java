package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowingResponse {
    private Long userNo;
    private String loginId;
    private String nickname;
    private String profileImageUrl;
    private Boolean isFollowing;

    public static FollowingResponse from(User user, Boolean isFollowing) {
        return FollowingResponse.builder()
                .userNo(user.getNo())
                .loginId(user.getLoginId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImage())
                .isFollowing(isFollowing)
                .build();
    }
}