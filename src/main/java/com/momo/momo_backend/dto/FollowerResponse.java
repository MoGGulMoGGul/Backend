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
    private Boolean isFollowing; // 현재 로그인한 사용자가 이 사용자를 팔로우하는지 여부

    public static FollowerResponse from(User user, Boolean isFollowing) {
        return FollowerResponse.builder()
                .userNo(user.getNo())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImage())
                .isFollowing(isFollowing)
                .build();
    }
}