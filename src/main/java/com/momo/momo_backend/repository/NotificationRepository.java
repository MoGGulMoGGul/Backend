package com.momo.momo_backend.repository;

import com.momo.momo_backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiver_NoOrderByCreatedAtDesc(Long receiverNo);
}
