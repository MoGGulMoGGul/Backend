package com.momo.momo_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
public class GroupListResponse {
    private Long groupNo;       // 그룹 식별 번호
    private String name;        // 그룹 이름
    private int memberCount;    // 그룹 멤버 수
}