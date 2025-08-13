package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);             // 변경
    Optional<User> findByNickname(String nickname);

    // 로그인 아이디로 사용자를 검색하는 메서드 추가 (LIKE 검색)
    List<User> findByLoginIdContainingIgnoreCase(String loginId);
}
