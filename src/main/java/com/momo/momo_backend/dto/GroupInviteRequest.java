package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupInviteRequest {
    private Long userId; // 초대할 사용자의 식별 번호 (User.no)
}