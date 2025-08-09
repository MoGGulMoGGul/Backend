package com.momo.momo_backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StorageUpdateResponse {
    private Long storageNo; // 보관함 식별 번호
    private String name;    // 수정된 보관함 이름
}