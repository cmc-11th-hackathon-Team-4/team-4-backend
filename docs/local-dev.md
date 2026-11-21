# 로컬 개발 환경

## 준비물

- Java 21
- Docker (MySQL, Redis 컨테이너용)

## 실행

```bash
docker compose up -d          # MySQL + Redis 를 띄운다 (docker-compose.yml)
./gradlew bootRun             # local 프로필로 실행 (application.yml 의 기본값)
```

- Swagger: <http://localhost:8080/swagger-ui/index.html>
- 헬스 체크: `curl localhost:8080/api/v1/health`

## 포트와 접속 정보

| 구성 요소 | 호스트 포트 | 접속 정보 | 비고 |
| --- | --- | --- | --- |
| Spring Boot | 8080 | - | |
| MySQL 8.4 | **3307** | DB `hackathon4` / 유저 `hackathon4` / 비밀번호 `hackathon4` (root 비밀번호 `root`) | 로컬에 이미 깔린 MySQL(3306)과 겹치지 않게 3307 |
| Redis 7.4 | **6380** | 비밀번호 없음 | 로컬에 이미 깔린 Redis(6379)와 겹치지 않게 6380 |

DataGrip 등 DB 툴은 위 MySQL 정보(`localhost:3307`)로 연결한다.

## 환경변수 (.env)

`application-local.yml` 은 프로젝트 루트의 `.env` 가 있으면 그 값을, 없으면 기본값을 쓴다.
기본값이 위 docker-compose 값과 같으므로 `.env` 없이도 동작한다.

```bash
cp .env.example .env   # 다른 DB 에 붙고 싶을 때만 값 수정
```

- `.env` 는 `.gitignore` 대상이라 커밋되지 않는다.
- 운영 값은 `.env` 가 아니라 GitHub Secret `APP_ENV` 로 넣는다 ([deploy.md](deploy.md#3-github-secrets)).
- Redis 접속 정보는 현재 `.env` 가 아니라 `application-local.yml` 에 고정돼 있다.

## 빌드·테스트

```bash
./gradlew build          # 컴파일 + Spotless 포맷 + 테스트
./gradlew spotlessApply  # 포맷만
```

- 테스트는 MySQL 없이 인메모리 H2 로 돈다 (`src/test/resources/application.yml`).
- 컴파일하면 Spotless 가 자동으로 포맷한다. CI 에서는 고치지 않고 검사만 한다.

## 자주 겪는 문제

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `Unable to determine Dialect without JDBC metadata` | MySQL 컨테이너가 꺼져 있다 (DB 에 연결 못 하면 Hibernate 가 이 메시지를 낸다) | `docker compose up -d` |
| Redis 관련 API 만 500 | Redis 컨테이너가 꺼져 있다 (앱 기동 시점에는 Redis 에 연결하지 않는다) | `docker compose up -d redis` |
