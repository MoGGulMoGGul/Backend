package com.momo.momo_backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StorageNameResponse {
    private Long storageNo; // 보관함 ID 추가
    private String name; // 보관함 이름
    private Long userNo; // 보관함 소유자 userNo 추가
}