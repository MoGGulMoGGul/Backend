package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Tip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TipRepository extends JpaRepository<Tip, Long> {

    /* ---------------- 기본 조회 ---------------- */
    List<Tip> findAllByIsPublicTrueOrderByCreatedAtDesc();
    List<Tip> findAllByUser_NoOrderByCreatedAtDesc(Long userNo);

    /* ---------------- 전체 공개 + 단일 키워드 ---------------- */
    @Query("""
        SELECT  t
        FROM    Tip t
        WHERE   t.isPublic = true
          AND  (LOWER(t.title)          LIKE LOWER(CONCAT('%', :kw, '%'))
             OR  LOWER(t.contentSummary) LIKE LOWER(CONCAT('%', :kw, '%'))
             OR  LOWER(t.url)            LIKE LOWER(CONCAT('%', :kw, '%')))
        ORDER BY t.createdAt DESC
    """)
    List<Tip> searchPublicTipsByKeyword(@Param("kw") String kw);

    /* ---------------- 그룹 보관함 + 단일 키워드 ---------------- */
    @Query("""
        SELECT DISTINCT t
        FROM   Tip t
          JOIN t.storageTips st
          JOIN st.storage    s
        WHERE  s.group.no = :groupId
          AND (LOWER(t.title)          LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(t.contentSummary) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(t.url)            LIKE LOWER(CONCAT('%', :kw, '%')))
        ORDER BY t.createdAt DESC
    """)
    List<Tip> searchGroupTipsByKeyword(@Param("groupId") Long groupId,
                                       @Param("kw")      String kw);

    /* ---------------- 내 ‘전체’ 보관함 + 단일 키워드 ---------------- */
    @Query("""
        SELECT DISTINCT t
        FROM   Tip t
          JOIN t.storageTips st
          JOIN st.storage    s
        WHERE  s.user.no = :userId
          AND (LOWER(t.title)          LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(t.contentSummary) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(t.url)            LIKE LOWER(CONCAT('%', :kw, '%')))
    """)
    List<Tip> searchUserStorageTipsByKeyword(@Param("userId") Long userId,
                                             @Param("kw")     String kw);

    /* ---------------- 태그 + 단일 키워드 ---------------- */
    @Query("""
        SELECT DISTINCT t
        FROM   Tip t
          JOIN t.tipTags tt
          JOIN tt.tag   tag
        WHERE  tag.name = :tagName
          AND (LOWER(t.title)          LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(t.contentSummary) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(t.url)            LIKE LOWER(CONCAT('%', :kw, '%')))
    """)
    List<Tip> searchTagTipsByKeyword(@Param("tagName") String tagName,
                                     @Param("kw")      String kw);

    /* ---------------- 특정 보관함(ID) + 단일 키워드 ---------------- */
    @Query("""
        SELECT DISTINCT st.tip
        FROM   StorageTip st
        WHERE  st.storage.no = :storageId
          AND (LOWER(st.tip.title)          LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(st.tip.contentSummary) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(st.tip.url)            LIKE LOWER(CONCAT('%', :kw, '%')))
    """)
    List<Tip> searchStorageTipsByKeyword(@Param("storageId") Long storageId,
                                         @Param("kw")        String kw);

    /* ====== 기존 단순 조회 ====== */
    @Query("""
        SELECT  t
        FROM    Tip t
          JOIN  t.storageTips st
          JOIN  st.storage    s
        WHERE   s.group.no = :groupId
        ORDER BY t.createdAt DESC
    """)
    List<Tip> findTipsByGroupId(@Param("groupId") Long groupId);

    @Query("""
        SELECT  t
        FROM    Tip t
          JOIN  t.storageTips st
          JOIN  st.storage    s
        WHERE   s.user.no = :userId
        ORDER BY t.createdAt DESC
    """)
    List<Tip> findTipsByUserStorage(@Param("userId") Long userId);

    @Query("""
        SELECT  st.tip
        FROM    StorageTip st
        WHERE   st.storage.no = :storageId
        ORDER BY st.tip.createdAt DESC
    """)
    List<Tip> findTipsByStorageId(@Param("storageId") Long storageId);

    @Query("""
        SELECT  t
        FROM    Tip t
          JOIN  t.tipTags tt
          JOIN  tt.tag tag
        WHERE   tag.name = :tagName
        ORDER BY t.createdAt DESC
    """)
    List<Tip> findTipsByTagName(@Param("tagName") String tagName);
}
