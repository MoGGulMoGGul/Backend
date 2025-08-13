package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import com.momo.momo_backend.enums.NotificationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no; // 알림 식별 번호

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_no", nullable = false)
    private User receiver; // 알림 수신자 (User.no)

    // optional = true, nullable = true로 변경하여 null 값을 허용하도록 수정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tip_no")
    private Tip tip; // 관련된 팁 번호 (Tip.no)

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 알림 발생 시각


    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;
}