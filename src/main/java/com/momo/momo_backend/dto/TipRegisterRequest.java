package com.momo.momo_backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List; // List 임포트

@Getter
@Setter
public class TipRegisterRequest {
    private Long tipId; // 생성 단계에서 받은 꿀팁 ID
    private Boolean isPublic; // 공개 여부 (등록 단계에서 추가)
    private Long storageId;   // 보관함 ID (등록 단계에서 추가)
    // 필요하다면, 생성 단계에서 받은 title, contentSummary, tags 등도 여기에 포함하여
    // 클라이언트가 최종적으로 등록할 때 모든 정보를 한 번에 보낼 수도 있습니다.
    // 현재는 tipId로 기존 팁을 찾아 업데이트하는 방식으로 진행합니다.
}
