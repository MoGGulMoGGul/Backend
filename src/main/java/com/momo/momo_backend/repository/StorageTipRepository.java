package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Storage;
import com.momo.momo_backend.entity.StorageTip;
import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface StorageTipRepository extends JpaRepository<StorageTip, Long> {
    // 꿀팁이 보관함에 이미 저장되었는지 확인하기 위한 메서드
    boolean existsByStorageAndTip(Storage storage, Tip tip);

    // 사용자와 꿀팁 기준으로 모든 보관함-꿀팁 관계를 삭제하는 쿼리 추가
    @Modifying
    @Query("DELETE FROM StorageTip st WHERE st.tip = :tip AND st.storage.user = :user")
    void deleteByTipAndUser(@Param("tip") Tip tip, @Param("user") User user);
}