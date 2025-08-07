package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TipRequest {
    private String url;
    private String title;       // optional
    private List<String> tags;  // optional
    private Boolean isPublic;
    private Long storageId;
}