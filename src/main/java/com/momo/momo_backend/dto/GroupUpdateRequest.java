package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupUpdateRequest {
    private String name; // 변경할 그룹 이름
}