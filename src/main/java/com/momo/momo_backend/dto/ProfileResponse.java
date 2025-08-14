package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileResponse {
    private Long userNo;
    private String loginId;
    private String nickname;
    private String profileImageUrl;
    private Long followerCount;
    private Long followingCount;
    private Long totalBookmarkCount; // 사용자의 꿀팁이 즐겨찾기된 총 횟수
    private Boolean isFollowing;

    // User 엔티티와 관련 카운트들을 받아 DTO를 생성하는 정적 팩토리 메서드
    public static ProfileResponse from(User user, Long followerCount, Long followingCount, Long totalBookmarkCount, Boolean isFollowing) {
        return ProfileResponse.builder()
                .userNo(user.getNo())
                .loginId(user.getLoginId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImage())
                .followerCount(followerCount)
                .followingCount(followingCount)
                .totalBookmarkCount(totalBookmarkCount)
                .isFollowing(isFollowing)
                .build();
    }
}