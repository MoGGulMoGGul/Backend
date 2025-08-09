package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter; // Lombok Setter 추가

@Getter
@Setter // 그룹 이름을 설정할 수 있도록 Setter 추가
public class GroupCreateRequest {
    private String name; // 그룹 이름
}