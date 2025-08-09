package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.GroupCreateRequest;
import com.momo.momo_backend.dto.GroupInviteRequest;
import com.momo.momo_backend.dto.GroupListResponse;
import com.momo.momo_backend.entity.Group;
import com.momo.momo_backend.entity.GroupMember; // GroupMember 임포트
import com.momo.momo_backend.entity.User; // User 임포트
import com.momo.momo_backend.repository.GroupRepository;
import com.momo.momo_backend.repository.GroupMemberRepository; // GroupMemberRepository 임포트
import com.momo.momo_backend.repository.UserRepository; // UserRepository 임포트
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Transactional 임포트

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository; // 사용자 정보 조회를 위해 추가
    private final GroupMemberRepository groupMemberRepository; // 그룹 멤버 저장을 위해 추가

    // 그룹 생성
    @Transactional // 트랜잭션 관리
    public Group createGroup(GroupCreateRequest request, Long userNo) {
        // 1. 그룹 이름 중복 확인 (선택 사항이지만, 실제 서비스에서는 고려할 수 있음)
        // if (groupRepository.findByName(request.getName()).isPresent()) {
        //     throw new IllegalArgumentException("이미 존재하는 그룹 이름입니다.");
        // }

        // 2. 그룹 생성
        Group group = new Group();
        group.setName(request.getName());
        // createdAt은 @Column(name = "created_at", updatable = false) 및 @Builder.Default로 자동 설정됨

        Group savedGroup = groupRepository.save(group);

        // 3. 그룹 생성자를 그룹 멤버로 추가
        User creator = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("그룹 생성 사용자가 존재하지 않습니다."));

        GroupMember groupMember = GroupMember.builder()
                .group(savedGroup)
                .user(creator)
                .build();
        groupMemberRepository.save(groupMember);

        return savedGroup;
    }

    // 그룹에 사용자 초대
    @Transactional // 트랜잭션 관리
    public void inviteMember(Long groupId, GroupInviteRequest request, Long inviterUserNo) {
        // 1. 그룹 존재 여부 확인
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다."));

        // 2. 초대할 사용자 존재 여부 확인
        User invitedUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("초대할 사용자가 존재하지 않습니다."));

        // 3. 초대하는 사용자가 그룹 멤버인지 확인 (선택 사항: 그룹 관리자만 초대 가능하도록 할 수도 있음)
        // 현재는 그룹에 속한 아무 멤버나 초대할 수 있다고 가정합니다.
        boolean inviterIsMember = groupMemberRepository.existsByGroupAndUser(group, userRepository.findById(inviterUserNo)
                .orElseThrow(() -> new IllegalArgumentException("초대하는 사용자가 존재하지 않습니다.")));
        if (!inviterIsMember) {
            throw new IllegalArgumentException("그룹 멤버만 다른 사용자를 초대할 수 있습니다.");
        }

        // 4. 초대할 사용자가 이미 그룹 멤버인지 확인
        if (groupMemberRepository.existsByGroupAndUser(group, invitedUser)) {
            throw new IllegalArgumentException("해당 사용자는 이미 그룹의 멤버입니다.");
        }

        // 5. GroupMember 엔티티 생성 및 저장
        GroupMember newMember = GroupMember.builder()
                .group(group)
                .user(invitedUser)
                .build(); // joinedAt은 @Builder.Default로 자동 설정됨
        groupMemberRepository.save(newMember);

        // TODO: 알림 기능 추가 (선택 사항)
        // NotificationService.sendGroupInviteNotification(invitedUser, group, inviterUser);
    }

    // 그룹 멤버 나가기
    @Transactional // 트랜잭션 관리
    public void leaveGroup(Long groupId, Long userNo) {
        // 1. 그룹 존재 여부 확인
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다."));

        // 2. 나갈 사용자 존재 여부 확인
        User userToLeave = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        // 3. 사용자가 해당 그룹의 멤버인지 확인
        GroupMember groupMember = groupMemberRepository.findByGroupAndUser(group, userToLeave)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자는 이 그룹의 멤버가 아닙니다."));

        // 4. 그룹 멤버 삭제
        groupMemberRepository.delete(groupMember);
    }

    // 그룹 멤버 조회
    @Transactional(readOnly = true)
    public List<User> getGroupMembers(Long groupId, Long requestingUserNo) {
        // 1. 그룹 존재 여부 확인
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다."));

        // 2. 요청하는 사용자가 그룹 멤버인지 확인
        User requestingUser = userRepository.findById(requestingUserNo)
                .orElseThrow(() -> new IllegalArgumentException("요청하는 사용자가 존재하지 않습니다.")); // 인증된 사용자이므로 보통 발생하지 않음

        if (!groupMemberRepository.existsByGroupAndUser(group, requestingUser)) {
            // ✨ 그룹 멤버가 아니면 AccessDeniedException 발생
            throw new AccessDeniedException("그룹 멤버만 그룹 멤버 목록을 조회할 수 있습니다.");
        }

        // 3. 해당 그룹의 모든 GroupMember 엔티티 조회
        List<GroupMember> groupMembers = groupMemberRepository.findByGroup(group);

        // 4. GroupMember에서 User 엔티티만 추출하여 반환
        return groupMembers.stream()
                .map(GroupMember::getUser)
                .collect(Collectors.toList());
    }

    // 그룹 목록 조회 (사용자가 속한 그룹)
    @Transactional(readOnly = true)
    public List<GroupListResponse> getGroupsForUser(Long userNo) {
        // 1. 사용자 존재 여부 확인 (인증된 사용자이므로 보통 발생하지 않음)
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        // 2. 해당 사용자가 속한 모든 GroupMember 엔티티 조회
        List<GroupMember> groupMemberships = groupMemberRepository.findAllByUser_No(userNo);

        // 3. 각 GroupMember에서 Group 정보를 추출하고, 해당 그룹의 멤버 수를 계산하여 DTO로 변환
        return groupMemberships.stream()
                .map(groupMember -> {
                    Group group = groupMember.getGroup();
                    int memberCount = groupMemberRepository.countByGroup(group); // 해당 그룹의 멤버 수 계산
                    return GroupListResponse.builder()
                            .groupNo(group.getNo())
                            .name(group.getName())
                            .memberCount(memberCount)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
