package com.momo.momo_backend.service;

import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.repository.TipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// 조회 전문 서비스

@Service
@RequiredArgsConstructor
public class TipQueryService {

    private final TipRepository tipRepository;

    // 사용자가 작성한 팁 조회
    public List<Tip> getTipsByUser(Long userNo) {
        return tipRepository.findAllByUser_No(userNo);
    }

    // 팁 목록 조회 (공개된 팁만)
    public List<Tip> getAllPublicTips() {
        return tipRepository.findAllByIsPublicTrue();
    }

    // 특정 보관함에 속한 팁 조회
    public List<Tip> getTipsByStorage(Long storageNo) {
        return tipRepository.findTipsByStorageId(storageNo);
    }

    // 상세 팁 조회
    public Tip getTipDetails(Long tipNo) {
        return tipRepository.findById(tipNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 꿀팁이 존재하지 않습니다."));
    }
}