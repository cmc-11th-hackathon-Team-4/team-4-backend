# 0010. Redis 를 쓰지 않아 제거

- 상태: 채택
- 날짜: 2026-11-22

## 배경

[ADR 0003](0003-redis-on-ec2.md) 에서 용도를 정하지 않은 채 Redis 를 넣었다. 지금까지 Redis 를 쓰는 기능이 없다.

## 결정

- `spring-boot-starter-data-redis` 의존성, 로컬 설정(`application-local.yml`), docker-compose 의 `redis` 서비스를 지운다.
- `scripts/ec2-setup.sh` 에서 Redis 설치를 뺀다.

## 결과

- 앱·로컬 환경이 MySQL 하나만 필요하다.
- 이미 세팅된 운영 서버에는 `redis-server` 가 남아 있다 (앱은 더 이상 붙지 않음). 지우려면 서버에서 `sudo systemctl disable --now redis-server` 후 패키지를 삭제한다.
- 나중에 캐시 등이 필요해지면 새 ADR 로 다시 결정한다.
