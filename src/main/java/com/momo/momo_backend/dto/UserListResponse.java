package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserListResponse {
    private Long userNo;
    private String nickname;
    private String profileImageUrl;

    public static UserListResponse from(User user) {
        return UserListResponse.builder()
                .userNo(user.getNo())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImage())
                .build();
    }
}