package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByLoginId(String loginId);   // 변경

    /* FK 기반 삭제 */
    @Modifying
    @Query("delete from UserCredential uc where uc.user.no = :userNo")
    void deleteByUser_No(Long userNo);
}
