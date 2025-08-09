package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.User; // User 엔티티 임포트
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor; // AllArgsConstructor 추가

@Getter
@Builder
@AllArgsConstructor
public class GroupMemberResponse {
    private Long userNo;    // 사용자 식별 번호
    private String nickname; // 사용자 닉네임

    // User 엔티티로부터 DTO를 생성하는 팩토리 메서드
    public static GroupMemberResponse from(User user) {
        return GroupMemberResponse.builder()
                .userNo(user.getNo())
                .nickname(user.getNickname())
                .build();
    }
}