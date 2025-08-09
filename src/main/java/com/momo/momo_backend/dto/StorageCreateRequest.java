package com.momo.momo_backend.dto;

import lombok.Getter;

@Getter
public class StorageCreateRequest {
    private String name;
    private Long groupNo;  // nullable
}