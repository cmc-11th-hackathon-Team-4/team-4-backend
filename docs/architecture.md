# 아키텍처

## 구성 요소와 요청 흐름

```
[브라우저 / 프론트 (Vercel)]
        │ HTTPS
        ▼
[Caddy :443]  TLS 종료, Let's Encrypt 자동 갱신, X-Forwarded-* 전달
        │ http://localhost:8080
        ▼
[Spring Boot (systemd: team4-backend)]
   ├─ CORS: app.cors.allowed-origin-patterns 에 있는 출처만 허용
   ├─ Controller → Service → Repository
   └──▶ [RDS MySQL]          (JPA, ddl-auto: update)
```

| 구성 요소 | 로컬 | 운영 |
| --- | --- | --- |
| 앱 | `./gradlew bootRun` (`local` 프로필) | EC2 systemd, `--spring.profiles.active=prod` |
| MySQL | docker-compose `localhost:3307` | RDS (`APP_ENV` 의 `DB_*`) |
| 리버스 프록시 | 없음 | Caddy |
| 테스트 | H2 인메모리 | - |

## 디렉터리 구조: 도메인 먼저, 그 안에서 계층별로 나눈다

**기능은 도메인 패키지 아래에 모으고, 도메인 안을 계층 하위 패키지로 나눈다** ([ADR 0007](adr/0007-domain-then-layer-packages.md)).
최상위를 계층(controller/service/repository)별로 나누지 않는다.

```
src/main/java/com/cmc/hackathon4/
├── BackendApplication.java
├── domain/                         # 기능(도메인)별 패키지
│   ├── health/                     #   가장 단순한 형태 (DB 없음)
│   │   ├── controller/   HealthController, HealthApiDocs
│   │   └── dto/          HealthResponse
│   ├── chore/                      #   하위 패키지 6개를 모두 갖춘 기준 예시
│   │   ├── controller/   ChoreController, ChoreApiDocs (Swagger 어노테이션은 ApiDocs 에만)
│   │   ├── service/      ChoreService
│   │   ├── repository/   ChoreRepository
│   │   ├── entity/       Chore
│   │   ├── converter/    ChoreConverter (toEntity / toResponse, static)
│   │   └── dto/          ChoreCreateRequest, ChoreResponse ... (record)
│   └── ...                         #   user, house, period, assignment, transfer, notification
└── global/                         # 여러 도메인이 함께 쓰는 것만
    ├── config/                 #   JpaAuditingConfig, SwaggerConfig, WebConfig(CORS)
    ├── entity/                 #   BaseTimeEntity (createdAt / updatedAt)
    ├── exception/              #   BusinessException, ErrorCode, GlobalExceptionHandler
    ├── swagger/                #   @ApiErrorCodes, @ApiErrorCase, 에러 응답 자동 생성
    └── response/               #   ApiResponse (공통 응답 형식)
```

규칙:

- 새 기능 = `domain/{도메인명}/` 새 패키지. 도메인명은 영어 소문자 한 단어(필요하면 붙여 쓰기, 예: `chore`).
- 도메인 안은 `controller/`(Controller, ApiDocs), `service/`, `repository/`, `entity/`(엔티티), `converter/`(DTO↔Entity 변환, [ADR 0008](adr/0008-converter-classes.md)), `dto/`(Request/Response record) 로 나눈다. 클래스가 없는 하위 패키지는 만들지 않는다.
- `global/` 에는 둘 이상의 도메인이 쓰는 것만 둔다. 한 도메인에서만 쓰는 클래스는 그 도메인 패키지에 둔다.
- 이름·코드 규칙은 [AGENTS.md](../AGENTS.md) 를 따른다.

## 공통 응답과 예외

- 모든 API 는 `ApiResponse` 형식으로 응답한다.
  - 성공 `{ "success": true, "code": "OK", "message": "성공", "data": ... }`
  - 실패 `{ "success": false, "code": "COMMON-400", "message": "...", "data": null }`
- 서비스는 실패 시 `BusinessException(ErrorCode.XXX)` 을 던지고, `GlobalExceptionHandler` 가 위 형식으로 바꾼다.
- `ErrorCode` 코드 형식: `{도메인}-{HTTP 상태}` (예: `TEST-ITEM-404`), 공통은 `COMMON-{상태}`.

### Swagger 에러 문서 자동화 (`global/swagger/`)

- Swagger 첫 화면 설명에 공통 응답 형식, `X-Device-Id`, 시간 형식, **`ErrorCode` 전체 표**가 나온다. 표는 enum 에서 자동 생성된다 (`SwaggerConfig`).
- 각 API 의 에러 응답은 `XxxApiDocs` 의 `@ApiErrorCodes` / `@ApiErrorCase` 를 `ErrorCodeOperationCustomizer` 가 읽어 만든다.
  HTTP 상태별로 묶고, 코드마다 실제 응답과 같은 예시(`ErrorResponse` 스키마)를 넣는다.
- 자동으로 붙는 것: 요청 본문이 있으면 `COMMON-400`, `@CurrentUser` 가 있으면 `COMMON-400`(헤더 없음)·`USER-404`(미등록 기기, `global/auth/UserWebConfig`).
- 예시의 `message` 는 `ErrorCode` 기본 메시지다. 서비스가 `BusinessException(code, 메시지)` 로 다른 메시지를 주면 실제 응답의 `message` 는 다를 수 있다. 분기는 `code` 로 한다.

## 설정 파일 역할

| 파일 | 적용 | 내용 |
| --- | --- | --- |
| `src/main/resources/application.yml` | 항상 | 기본 프로필(`local`), JPA 공통, **CORS 허용 출처** |
| `src/main/resources/application-local.yml` | 로컬 | docker-compose DB, `.env` 읽기 |
| `src/main/resources/application-prod.yml` | 운영 | 환경변수(`DB_*`)로 RDS, 프록시 헤더 처리 |
| `src/test/resources/application.yml` | 테스트 | H2. 메인 `application.yml` 을 **통째로 대체**한다 |
| `.env.example` | 로컬 | `.env` 템플릿 |
| `docker-compose.yml` | 로컬 | MySQL |
| `scripts/ec2-setup.sh` | 운영 서버 | Java, Caddy, systemd, 배포 스크립트 설치 |
| `.github/workflows/deploy.yml` | CI | `main` push 시 배포 |
| `.github/workflows/setup-https.yml` | CI (수동) | 서버에 `ec2-setup.sh <도메인>` 적용 |
| `.github/workflows/pr-check.yml` | CI (PR) | `main` 대상 PR 마다 `scripts/check-docs.sh` 실행 (배포·AWS 권한 없음) |

## 설계 결정

[docs/adr/](adr/README.md)
