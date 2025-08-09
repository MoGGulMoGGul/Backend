package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Storage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorageRepository extends JpaRepository<Storage, Long> {
    List<Storage> findAllByUser_No(Long userNo);

    // 특정 사용자의 개인 보관함만 조회하는 메서드 추가
    List<Storage> findAllByUser_NoAndGroupIsNull(Long userNo);

    // 특정 그룹에 속한 보관함들을 조회하는 메서드 추가
    List<Storage> findAllByGroup_No(Long groupNo);
}
