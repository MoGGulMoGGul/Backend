package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findById(String id);  // 로그인용 ID 조회
}
