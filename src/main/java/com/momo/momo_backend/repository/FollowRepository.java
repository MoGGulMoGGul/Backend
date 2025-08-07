package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Follow;
import com.momo.momo_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    /* ====== 파생 쿼리 ====== */
    boolean existsByFollowerAndFollowing(User follower, User following);   // 중복 팔로우 방지
    List<Follow> findByFollowing(User following);                          // 나를 팔로우한 Follow 목록

    /* ====== JPQL ====== */
    @Query("SELECT f.follower FROM Follow f WHERE f.following.no = :userNo")
    List<User> findFollowersByUserNo(@Param("userNo") Long userNo);        // 나를 팔로우하는 ‘User’들
}
