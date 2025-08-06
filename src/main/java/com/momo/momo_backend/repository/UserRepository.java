package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);             // 변경
    Optional<User> findByNickname(String nickname);
}
