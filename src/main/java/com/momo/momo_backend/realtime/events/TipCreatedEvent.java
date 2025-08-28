package com.momo.momo_backend.realtime.events;

/** DB 커밋 이후 새 꿀팁이 생겼음을 알리는 최소 이벤트 */
public record TipCreatedEvent(Long tipId) {}
