package com.momo.momo_backend.realtime.events;

/** DB 커밋 이후 꿀팁 내용(요약/태그/썸네일 등)이 갱신되었음을 알리는 이벤트 */
public record TipUpdatedEvent(Long tipId) {}
