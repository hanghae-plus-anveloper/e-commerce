# E-commerce Asynchronous Design

> `Redis` 기반으로 비즈니스 로직 '선착순'이 보장되는 기능을 설계한다. 해당 설계를 기반으로 개발 및 구현한다.

## Redis 기반 선착순 쿠폰 발급 설계

### 문제 정의

- 기존에 조건부 업데이트 쿼리를 통해서 초과 발급은 방지하였으나, 선착순은 보장되지 않음
- 트래픽이 몰리는 상황에서 Redis를 활용한 분산환경 및 선착순 제어 로직 추가 필요

### 설계 목표

1. 선착순 보장: 초과된 요청에 대하여 즉시 차단
2. 비동기 확정: 쿠폰 엔티티를 생성하는 확정 로직은 큐/비동기 프로세스에서 수행
3. 장애 격리: Redis 장애 시 우아한 폴백(graceful fallback) 설계 - 학습

### Key & 자료구조

- 남은 수량 관리
  - key: `COUPON:POLICY:{policyId}:REMAINING`
  - 자료구조: `String` 에 최초 발급 수량 저장
    - `DECR`로 원자적 차감
    - expire 

- 선착순 관리
  - key: `COUPON:POLICY:{policyId}:PENDING`
  - 자료구조: `ZSET`(`userId`, `timestamp`)
    - Redis 선착순 통과 직후 사용자를 넣음
    - score: 요청 시간 저장 → "누가 몇 번째로 도착했는지" 순서 관리 가능
    - consumer가 DB 반영 후 ISSUED로 이동.
  - key: `COUPON:POLICY:{policyId}:ISSUED`
  - 자료구조: `SET`(`userId`)
    - DB 반영 완료된 사용자
    - 중복 요청 차단 및 발급 최종 이력 확인용

## 통합 테스트

