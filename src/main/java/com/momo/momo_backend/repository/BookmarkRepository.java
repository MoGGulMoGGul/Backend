package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Bookmark;
import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    boolean existsByUser_NoAndTip_No(Long userNo, Long tipNo);

    // ✅ 즐겨찾기 중복 방지용 메서드
    boolean existsByUserAndTip(User user, Tip tip);

    // (선택) 즐겨찾기 삭제 시 유용
    void deleteByUser_NoAndTip_No(Long userNo, Long tipNo);
}