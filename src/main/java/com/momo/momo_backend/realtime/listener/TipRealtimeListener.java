package com.momo.momo_backend.realtime.listener;

import com.momo.momo_backend.realtime.RealtimeProperties;
import com.momo.momo_backend.realtime.events.TipCreatedEvent;
import com.momo.momo_backend.realtime.events.TipUpdatedEvent;
import com.momo.momo_backend.realtime.support.EventPayloadFactory;
import com.momo.momo_backend.realtime.support.TipQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TipRealtimeListener {

    private final SimpMessagingTemplate messaging;
    private final EventPayloadFactory payloadFactory;
    private final RealtimeProperties props;
    private final TipQueryPort tipQuery; // ✅ 추가

    /** 등록 직후: 가능하면 full, 불가능하면 minimal */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTipCreated(TipCreatedEvent e) {
        try {
            var view = tipQuery.findSummaryById(e.tipId());
            var evt = payloadFactory.tipNewFromView(view);
            messaging.convertAndSend(props.getTopics().getFeed(), evt);
        } catch (Exception ignored) {
            var evt = payloadFactory.tipNewMinimal(e.tipId());
            messaging.convertAndSend(props.getTopics().getFeed(), evt);
        }
    }

    /** 요약/썸네일 등 갱신 시: 항상 full로 업데이트 이벤트 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTipUpdated(TipUpdatedEvent e) {
        var view = tipQuery.findSummaryById(e.tipId());
        var evt = payloadFactory.tipUpdateFromView(view);
        messaging.convertAndSend(props.getTopics().getFeed(), evt);
    }
}
