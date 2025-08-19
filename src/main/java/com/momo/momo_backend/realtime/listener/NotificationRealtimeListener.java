package com.momo.momo_backend.realtime.listener;

import com.momo.momo_backend.realtime.RealtimeProperties;
import com.momo.momo_backend.realtime.events.NotificationCreatedEvent;
import com.momo.momo_backend.realtime.support.EventPayloadFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationRealtimeListener {

    private final SimpMessagingTemplate messaging;
    private final EventPayloadFactory payloadFactory;
    private final RealtimeProperties props;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent e) {
        var evt = payloadFactory.notificationNew(e.tipId(), e.message(), e.createdAt());
        // convertAndSendToUser의 첫번째 인자는 Principal.getName()과 동일해야 함 (Step3에서 userId 문자열을 name으로 세팅)
        messaging.convertAndSendToUser(
                String.valueOf(e.targetUserId()),
                props.getTopics().getUserNotificationQueue(),
                evt
        );
    }
}
