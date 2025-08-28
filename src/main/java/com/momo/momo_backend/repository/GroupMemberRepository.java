package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Group;
import com.momo.momo_backend.entity.GroupMember;
import com.momo.momo_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    // 그룹과 사용자로 GroupMember 존재 여부 확인 (중복 초대 방지 및 멤버 확인용)
    boolean existsByGroupAndUser(Group group, User user);

    // 그룹과 사용자로 GroupMember 엔티티 조회 (나가기 기능에서 사용)
    Optional<GroupMember> findByGroupAndUser(Group group, User user);

    // 특정 그룹의 모든 GroupMember 조회
    List<GroupMember> findByGroup(Group group);

    // 특정 사용자가 속한 모든 GroupMember 엔티티 조회
    List<GroupMember> findAllByUser_No(Long userNo);

    // 특정 그룹의 멤버 수 계산
    int countByGroup(Group group);

    // 그룹 번호와 사용자 번호로 GroupMember 존재 여부 확인
    boolean existsByGroup_NoAndUser_No(Long groupNo, Long userNo);
}
