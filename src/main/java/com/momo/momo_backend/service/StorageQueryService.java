package com.momo.momo_backend.service;

import com.momo.momo_backend.entity.Storage;
import com.momo.momo_backend.repository.StorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StorageQueryService {

    private final StorageRepository storageRepository;

    public List<Storage> findByUser(Long userNo) {
        return storageRepository.findAllByUser_No(userNo);
    }
}