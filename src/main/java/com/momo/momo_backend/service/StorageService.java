package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.StorageCreateRequest;
import com.momo.momo_backend.dto.StorageUpdateRequest;
import com.momo.momo_backend.entity.Storage;
import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.entity.Group;
import com.momo.momo_backend.repository.GroupRepository;
import com.momo.momo_backend.repository.StorageRepository;
import com.momo.momo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageRepository storageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    // 보관함 생성
    public Storage create(StorageCreateRequest request, Long loginUserNo) {
        User user = userRepository.findById(loginUserNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        Storage storage = Storage.builder()
                .user(user)
                .name(request.getName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 그룹 정보가 있는 경우에만 설정
        if (request.getGroupNo() != null) {
            Group group = groupRepository.findById(request.getGroupNo())
                    .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다."));
            storage.setGroup(group);
        }

        return storageRepository.save(storage);
    }

    // 보관함 수정
    @Transactional // 트랜잭션 관리
    public Storage update(Long storageNo, Long loginUserNo, StorageUpdateRequest request) {
        // 보관함이 존재하는지 확인
        Storage storage = storageRepository.findById(storageNo)
                .orElseThrow(() -> new IllegalArgumentException("보관함이 존재하지 않습니다."));

        // 로그인한 사용자가 보관함의 소유자인지 확인하여 수정 권한 검사
        if (!storage.getUser().getNo().equals(loginUserNo)) {
            throw new AccessDeniedException("해당 보관함에 대한 수정 권한이 없습니다.");
        }

        // 요청에서 받은 이름으로 보관함 이름 업데이트
        storage.setName(request.getName());
        // 업데이트된 보관함 엔티티 저장 및 반환
        return storageRepository.save(storage);
    }

    // 보관함 삭제
    public void delete(Long storageNo, Long loginUserNo) {
        Storage storage = storageRepository.findById(storageNo)
                .orElseThrow(() -> new IllegalArgumentException("보관함이 존재하지 않습니다."));
        if (!storage.getUser().getNo().equals(loginUserNo)) {
            throw new AccessDeniedException("해당 보관함에 대한 삭제 권한이 없습니다.");
        }
        storageRepository.delete(storage);
    }
}