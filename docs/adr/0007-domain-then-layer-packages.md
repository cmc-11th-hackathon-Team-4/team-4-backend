# 0007. 패키지는 도메인 먼저, 그 안에서 계층별로 나눈다

- 상태: 채택
- 날짜: 2026-11-22

## 배경

처음에는 `domain/{도메인}/` 한 패키지에 그 도메인의 클래스를 모두 평평하게 두었다.
조사해 보니 "도메인(기능)별로 먼저 나누는 것" 은 요즘 주류지만, 도메인 안을 하위 패키지 없이 평평하게 두는 방식은 소수(Spring Modulith 의 package-private 모듈 방식)이고,
국내 Spring 프로젝트에서는 `도메인/controller`, `도메인/service`, `도메인/dto` 처럼 도메인 안을 계층으로 다시 나누는 혼합형이 가장 흔하다.

## 결정

팀원에게 익숙한 주류 방식을 따른다.

```
domain/{도메인}/controller/   Controller, ApiDocs
domain/{도메인}/service/
domain/{도메인}/repository/
domain/{도메인}/entity/
domain/{도메인}/dto/
```

기존 `health`, `testitem`, `testredis` 도메인도 이 구조로 옮겼다 (API 경로·동작 변화 없음).

## 결과

- 처음 보는 팀원도 구조를 바로 이해한다. 도메인 안에서 계층별로 찾기 쉽다.
- 하위 패키지끼리 참조하려면 클래스가 `public` 이어야 해서, package-private 으로 다른 도메인의 접근을 막는 방식은 쓸 수 없다. 도메인 간 의존은 리뷰로 관리한다.
- 나중에 Spring Modulith 로 모듈 경계를 검증하려면 이 구조를 다시 검토해야 한다.
