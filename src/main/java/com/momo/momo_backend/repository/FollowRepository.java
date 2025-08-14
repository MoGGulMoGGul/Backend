package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Follow;
import com.momo.momo_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    /* ====== 파생 쿼리 ====== */
    boolean existsByFollowerAndFollowing(User follower, User following);   // 중복 팔로우 방지
    List<Follow> findByFollowing(User following);                          // 나를 팔로우한 Follow 목록

    /* ====== JPQL ====== */
    @Query("SELECT f.follower FROM Follow f WHERE f.following.no = :userNo")
    List<User> findFollowersByUserNo(@Param("userNo") Long userNo);        // 나를 팔로우하는 ‘User’들

    // 특정 사용자의 팔로잉 수를 계산하는 메서드 (내가 몇 명을 팔로우하는지)
    long countByFollower_No(Long userNo);

    // 특정 사용자의 팔로워 수를 계산하는 메서드 (나를 몇 명이 팔로우하는지)
    long countByFollowing_No(Long userNo);

    // 팔로우 관계를 찾기 위한 메서드 추가
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    // 특정 사용자가 팔로우하는 사용자 목록(팔로잉)을 조회하는 쿼리 추가
    @Query("SELECT f.following FROM Follow f WHERE f.follower.no = :userNo")
    List<User> findFollowingByUserNo(@Param("userNo") Long userNo);

    // 팔로우 상태 확인을 위한 메서드 추가
    boolean existsByFollower_NoAndFollowing_No(Long followerNo, Long followingNo);
}
