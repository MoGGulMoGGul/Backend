# ADR-0005: Realtime Stack (v1)

## Context
- 즉시성: 피드/랭킹/알림을 새로고침 없이 전달해야 함
- 기존 백엔드: Spring Boot + JWT 인증 + 도메인(Tip/Follow/Bookmark/Notification)
- 초기 트래픽: 단일 인스턴스 중심, 향후 스케일아웃 예상

## Decision
- Realtime 프로토콜: **STOMP over WebSocket**
- 서버: **Spring WebSocket** (핸드셰이크/채널 인터셉터로 JWT 인증)
- 엔드포인트: **`/ws`**
- 브로커 프리픽스: **`/topic`(브로드캐스트), `/queue`(개인 큐)**
- 앱 프리픽스: **`/app`**
- 유저 목적지 프리픽스: **`/user`**
- 채널 규칙:
    - Feed: **`/topic/feed`**
    - Challenge Rank: **`/topic/challenge/{id}/rank`**
    - User Notification Queue: **`/user/queue/notifications`**
- 확장 전략: 초기 **SimpleBroker**, 트래픽 증가 시 **Redis Pub/Sub 또는 브로커 릴레이(RabbitMQ 등)** 로 전환

## Security
- CONNECT 시 **JWT 검증 → Principal 주입**
- `/user/**` 큐는 인증 사용자만 접근
- CORS: Next.js 도메인만 허용(개발 중엔 와일드카드, 운영은 화이트리스트)

## Trade-offs
- SimpleBroker는 운영 규모에 한계 → 브로커 전환 필요 시 추가 운영비/구성 복잡도 증가
- SockJS 폴백은 필요 시점에만 적용(초기엔 native WS)

## Rollout Plan
1) v1: SimpleBroker + STOMP 경로 고정 + 이벤트 스키마 v1
2) v2: Redis Pub/Sub로 판올림(무중단 전환 가이드 포함)

## Out of Scope (이번 단계 제외)
- 실제 의존성 추가/설정 클래스/메시지 DTO/발행 로직(다음 단계에서)
