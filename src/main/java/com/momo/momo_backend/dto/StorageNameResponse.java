package com.momo.momo_backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StorageNameResponse {
    private String name; // 보관함 이름
}