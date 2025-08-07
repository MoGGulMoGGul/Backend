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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageRepository storageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    // 보관함 생성
    public Storage create(StorageCreateRequest request) {
        User user = userRepository.findById(request.getUserNo())
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        Storage storage = new Storage();
        storage.setUser(user);
        storage.setName(request.getName());
        storage.setCreatedAt(LocalDateTime.now());
        storage.setUpdatedAt(LocalDateTime.now());

        if (request.getGroupNo() != null) {
            Group group = groupRepository.findById(request.getGroupNo())
                    .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다."));
            storage.setGroup(group);
        }

        return storageRepository.save(storage);
    }

    // 보관함 수정
    public Storage update(Long storageNo, StorageUpdateRequest request) {
        Storage storage = storageRepository.findById(storageNo)
                .orElseThrow(() -> new IllegalArgumentException("보관함이 존재하지 않습니다."));

        storage.setName(request.getName());
        storage.setUpdatedAt(LocalDateTime.now());
        return storageRepository.save(storage);
    }

    // 보관함 삭제
    public void delete(Long storageNo) {
        Storage storage = storageRepository.findById(storageNo)
                .orElseThrow(() -> new IllegalArgumentException("보관함이 존재하지 않습니다."));

        storageRepository.delete(storage);
    }
}