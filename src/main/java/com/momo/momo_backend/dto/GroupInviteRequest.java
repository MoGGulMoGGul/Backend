package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GroupInviteRequest {
    private List<String> userLoginIds; // 초대할 사용자들의 로그인 아이디 목록 (User.loginId)
}