package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Tip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TipRepository extends JpaRepository<Tip, Long> {

    /* ====== 기본 조회 ====== */
    List<Tip> findAllByIsPublicTrueOrderByCreatedAtDesc();
    List<Tip> findAllByUser_NoOrderByCreatedAtDesc(Long userNo);

    /* ====== 그룹 보관함 ====== */
    @Query("""
           SELECT t
           FROM   Tip t
           JOIN   t.storageTips st
           JOIN   st.storage s
           WHERE  s.group.no = :groupId
           ORDER  BY t.createdAt DESC
           """)
    List<Tip> findTipsByGroupId(@Param("groupId") Long groupId);

    /* ====== 내 보관함(전체) ====== */
    @Query("""
           SELECT t
           FROM   Tip t
           JOIN   t.storageTips st
           JOIN   st.storage s
           WHERE  s.user.no = :userId
           ORDER  BY t.createdAt DESC
           """)
    List<Tip> findTipsByUserStorage(@Param("userId") Long userId);

    /* ====== 태그 ====== */
    @Query("""
           SELECT t
           FROM   Tip t
           JOIN   t.tipTags tt
           JOIN   tt.tag tag
           WHERE  tag.name = :tagName
           ORDER  BY t.createdAt DESC
           """)
    List<Tip> findTipsByTagName(@Param("tagName") String tagName);

    /* ====== 특정 보관함(ID) ====== */
    @Query("""
           SELECT st.tip
           FROM   StorageTip st
           WHERE  st.storage.no = :storageId
           ORDER  BY st.tip.createdAt DESC
           """)
    List<Tip> findTipsByStorageId(@Param("storageId") Long storageId);
    Optional<Tip> findByNoAndUser_No(Long tipNo, Long userNo);
    List<Tip> findAllByUser_No(Long userId);
    List<Tip> findAllByIsPublicTrue();
    List<Tip> findAllByStorage_No(Long storageId);
}