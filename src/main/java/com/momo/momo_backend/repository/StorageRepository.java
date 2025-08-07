package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Storage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorageRepository extends JpaRepository<Storage, Long> {
    List<Storage> findAllByUser_No(Long userNo);
}
