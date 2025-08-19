// src/main/java/com/momo/momo_backend/service/NotificationService.java
package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.NotificationResponse;
import com.momo.momo_backend.entity.Notification;
import com.momo.momo_backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate; // 메시지 전송을 위한 템플릿

    // 기존 알림 목록 조회 메서드
    public List<NotificationResponse> getNotifications(Long userNo) {
        List<Notification> notifications = notificationRepository.findByReceiver_NoOrderByCreatedAtDesc(userNo);
        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 알림을 생성하고 실시간으로 전송하는 메서드
    public void sendNotification(Notification notification) {
        notificationRepository.save(notification);

        NotificationResponse notificationResponse = NotificationResponse.fromEntity(notification);

        String username = notification.getReceiver().getLoginId();

        // 어떤 사용자에게 메시지를 보내는지 로그로 확인
        log.info("웹소켓 알림 전송 시도: 대상 = {}, 메시지 타입 = {}", username, notification.getType().name());

        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                notificationResponse
        );
        log.info("웹소켓 알림 전송 완료: 대상 = {}", username);
    }
}