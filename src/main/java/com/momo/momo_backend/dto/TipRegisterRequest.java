package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TipRegisterRequest {
    // 실제 필드명은 프로젝트 기준 유지
    private Long tipNo;
    private Long storageNo;
    private Boolean isPublic;

    /* ---- 호환용 alias: tipId/storageId도 받아주기 ---- */
    public Long getTipId() { return tipNo; }
    public void setTipId(Long tipId) { this.tipNo = tipId; }

    public Long getStorageId() { return storageNo; }
    public void setStorageId(Long storageId) { this.storageNo = storageId; }
}
