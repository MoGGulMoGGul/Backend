package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.TipTag;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TipTagRepository extends JpaRepository<TipTag, Long> {
    @Modifying
    @Query("delete from TipTag tt where tt.tip.no = :tipNo")
    void deleteByTipNo(@Param("tipNo") Long tipNo);

    List<TipTag> findByTip_No(Long tipNo);
}