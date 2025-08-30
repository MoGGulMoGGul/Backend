package com.momo.momo_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.momo.momo_backend.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no")
    private Long no;

    /** 수신자: notification.receiver_no */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_no", nullable = false)
    @JsonIgnore
    private User receiver;

    /** 관련 팁(없을 수 있음): notification.tip_no */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "tip_no")
    @JsonIgnore
    private Tip tip;

    /** enum을 VARCHAR(50)에 저장 */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    private NotificationType type;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void markAsRead() { this.read = true; }

    /** 팩토리: (수신자, 타입, 관련 팁[nullable]) */
    public static Notification of(User receiver, NotificationType type, Tip tip) {
        return Notification.builder()
                .receiver(receiver)
                .type(type)
                .tip(tip)
                .read(false)
                .build();
    }
}
