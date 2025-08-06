package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
