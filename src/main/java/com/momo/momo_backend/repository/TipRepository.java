package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Tip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipRepository extends JpaRepository<Tip, Long> {
    Optional<Tip> findByNoAndUser_No(Long tipNo, Long userNo);
    List<Tip> findAllByUser_No(Long userId);
    List<Tip> findAllByIsPublicTrue();
    List<Tip> findAllByStorage_No(Long storageId);
}