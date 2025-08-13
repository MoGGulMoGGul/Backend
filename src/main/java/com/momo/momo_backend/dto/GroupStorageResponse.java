package com.momo.momo_backend.dto;

import com.momo.momo_backend.entity.Storage; // Storage 엔티티 임포트
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
public class GroupStorageResponse {
    private Long storageNo; // 보관함 식별 번호
    private String name;    // 보관함 이름
    private Long userNo; // 보관함 생성자 userNo 추가

    // Storage 엔티티로부터 DTO를 생성하는 팩토리 메서드
    public static GroupStorageResponse from(Storage storage) {
        return GroupStorageResponse.builder()
                .storageNo(storage.getNo())
                .name(storage.getName())
                .userNo(storage.getUser().getNo()) // userNo 필드 추가
                .build();
    }
}