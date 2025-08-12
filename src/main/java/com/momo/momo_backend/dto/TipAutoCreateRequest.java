package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TipAutoCreateRequest {
    private String url;
    private Long storageId; // 꿀팁을 저장할 보관함 ID
}