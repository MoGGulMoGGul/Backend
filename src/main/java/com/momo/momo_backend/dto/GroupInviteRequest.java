package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GroupInviteRequest {
    private List<Long> userIds; // 초대할 사용자들의 식별 번호 목록 (User.no)
}