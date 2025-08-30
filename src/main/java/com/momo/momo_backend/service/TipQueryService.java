package com.momo.momo_backend.service;

import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.repository.TipRepository;
import com.momo.momo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 조회 전문 서비스
@Service
@RequiredArgsConstructor
public class TipQueryService {

    private final TipRepository tipRepository;
    private final UserRepository userRepository; // 사용자 정보 조회를 위해 추가

    /** 사용자가 작성한 팁 조회 (등록된 팁만) */
    @Transactional(readOnly = true)
    public List<Tip> getTipsByUser(Long userNo) {
        if (userNo == null) throw new IllegalArgumentException("userNo는 필수입니다.");
        return tipRepository.findRegisteredTipsByUserNo(userNo); // 등록된 팁만 조회
    }

    /** 팁 목록 조회 (공개된 팁만, 등록된 팁만) */
    @Transactional(readOnly = true)
    public List<Tip> getAllPublicTips() {
        return tipRepository.findAllPublicRegisteredTipsOrderByCreatedAtDesc(); // 등록된 공개 팁만 조회
    }

    /** 특정 보관함에 속한 팁 조회 (등록된 팁만) */
    @Transactional(readOnly = true)
    public List<Tip> getTipsByStorage(Long storageNo) {
        if (storageNo == null) throw new IllegalArgumentException("storageNo는 필수입니다.");
        return tipRepository.findTipsByStorageId(storageNo);
    }

    /** 상세 팁 조회 (등록 여부 무관) */
    @Transactional(readOnly = true)
    public Tip getTipDetails(Long tipNo) {
        if (tipNo == null) throw new IllegalArgumentException("tipNo는 필수입니다.");
        return tipRepository.findById(tipNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 꿀팁이 존재하지 않습니다."));
    }

    /** 특정 사용자의 공개 꿀팁 목록 조회 */
    @Transactional(readOnly = true)
    public List<Tip> getPublicTipsByUser(Long userNo) {
        if (userNo == null) throw new IllegalArgumentException("userNo는 필수입니다.");
        if (!userRepository.existsById(userNo)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        return tipRepository.findAllByUser_NoAndIsPublicTrueOrderByCreatedAtDesc(userNo);
    }
}
