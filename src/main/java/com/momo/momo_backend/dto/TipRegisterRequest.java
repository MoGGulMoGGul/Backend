package com.momo.momo_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class TipRegisterRequest {

    // 선택: 기존 생성 단계에서 온 정보가 있을 수 있음
    private String url;
    private String title;
    private String summary;
    private String thumbnailImageUrl;
    private List<String> tags;

    // 실제 등록에 필요한 핵심 파라미터
    private Long tipNo;              // 임시/생성된 팁을 등록할 때 사용 (nullable 가능)
    @NotNull
    private Long storageNo;          // 어느 보관함에 연결할지
    @NotNull
    private Boolean isPublic;        // 공개 여부

    /* ---- 호환용 alias: tipId/storageId도 받아주기 ---- */
    public Long getTipId() { return tipNo; }
    public void setTipId(Long tipId) { this.tipNo = tipId; }

    public Long getStorageId() { return storageNo; }
    public void setStorageId(Long storageId) { this.storageNo = storageId; }

    public Boolean getIsPublic() { return isPublic; }
}
