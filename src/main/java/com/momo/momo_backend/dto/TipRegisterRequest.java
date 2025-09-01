// src/main/java/com/momo/momo_backend/dto/TipRegisterRequest.java
package com.momo.momo_backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipRegisterRequest {

    // (선택) 미리보기에서 넘어올 수 있는 부가정보
    private String url;
    private String title;
    private String summary;
    private String thumbnailImageUrl;
    private List<String> tags;

    // 실제 등록에 필요한 값
    @NotNull @JsonAlias("tipId")
    private Long tipNo;

    @NotNull @JsonAlias("storageId")
    private Long storageNo;

    @NotNull
    private Boolean isPublic;

    /* ===== 컨트롤러 호환용 별칭 게터 ===== */
    public Long getTipId() { return tipNo; }
    public Long getStorageId() { return storageNo; }
}
