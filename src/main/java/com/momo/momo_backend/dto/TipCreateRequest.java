package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class TipCreateRequest {
    private String url;
    private String title;
    private List<String> tags;
}