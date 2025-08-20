package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TipRegisterRequest {
    private Long tipNo; // 생성 단계에서 받은 꿀팁 ID
    private Boolean isPublic; // 공개 여부
    private Long storageNo;   // 보관함 ID
}
