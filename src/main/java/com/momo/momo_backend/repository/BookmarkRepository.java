package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    boolean existsByUser_NoAndTip_No(Long userNo, Long tipNo);
}