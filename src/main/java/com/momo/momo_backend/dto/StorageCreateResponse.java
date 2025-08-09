package com.momo.momo_backend.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class StorageCreateResponse {
    private String message;
    private Long storageNo;
}