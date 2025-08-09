package com.momo.momo_backend.service;

import com.momo.momo_backend.entity.Storage;
import com.momo.momo_backend.repository.StorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import com.momo.momo_backend.entity.User; // User 엔티티 임포트
import com.momo.momo_backend.repository.UserRepository; // UserRepository 임포트


// 보관함 조회 전문 서비스

@Service
@RequiredArgsConstructor
public class StorageQueryService {

    private final StorageRepository storageRepository;
    private final UserRepository userRepository;

    public List<Storage> findByUser(Long userNo) {
        // 1. 사용자가 존재하는지 먼저 확인
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        // 2. 사용자가 존재하면 해당 사용자의 보관함 목록 조회
        return storageRepository.findAllByUser_No(userNo);
    }
}