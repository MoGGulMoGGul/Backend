package com.momo.momo_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;

@Getter
@AllArgsConstructor
@Builder
public class MessageResponse {
    private String message;
}
