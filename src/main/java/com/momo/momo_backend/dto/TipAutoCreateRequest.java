package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TipAutoCreateRequest {
    private Long storageId;
    private String url;
}
