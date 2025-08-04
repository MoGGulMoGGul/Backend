package com.momo.momo_backend.entity;

import jakarta.persistence.*;
import lombok.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_no", nullable = false)
    private User receiver;// 알림 수신자 (User.no)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tip_no", nullable = false)
    private Tip tip; // 관련된 팁 번호 (Tip.no)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 알림 발생 시각

    @Column(name = "type", nullable = false, length = 50)
    private String type; // 알림 타입: FOLLOW, TIP_UPLOAD 등

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false; // 읽음 여부
}