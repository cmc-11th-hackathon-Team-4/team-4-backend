# AGENTS.md

Spring Boot 4 / Java 21 / Lombok / JPA + MySQL. 기존 코드 패턴을 먼저 보고 따른다 (예시: `domain/chore` 가 하위 패키지 6개를 모두 갖춘 형태, `global/`).
구성·흐름은 `docs/architecture.md`, 로컬 실행은 `docs/local-dev.md`, 배포는 `docs/deploy.md`, 결정 이유는 `docs/adr/`.

## 필수
- 작업 후 `./gradlew build` 통과 확인. `main` 에 머지(push)되면 즉시 운영 배포되므로 직접 커밋 금지.
- 요청 없이 수정 금지 (요청으로 수정했다면 PR 본문에 그 사실과 배포 영향을 적는다):

  | 대상 | 이유 |
  | --- | --- |
  | `application-prod.yml` | 운영 설정. 머지 즉시 운영에 반영 |
  | `.github/` | 배포 파이프라인과 AWS 권한 사용 |
  | `scripts/` | 운영 서버 설치 내용. 바꾸면 서버에서 다시 실행해야 반영 (`docs/deploy.md`) |
  | `docker-compose.yml` | 팀 전원의 로컬 환경 |
  | `global/` 공통 클래스 (`ErrorCode` 추가는 허용) | 모든 도메인에 영향 |

## 디렉터리 구조: 도메인 먼저, 그 안에서 계층별로 분리
- **기능은 `domain/{도메인}/` 아래에 모은다.** 최상위를 계층별(`controller/`, `service/` …)로 나누지 않는다.
- 도메인 안은 아래 계층 하위 패키지로 나눈다. 해당 클래스가 없는 하위 패키지는 만들지 않는다.

  | 하위 패키지 | 들어가는 것 |
  | --- | --- |
  | `controller/` | `XxxController`, `XxxApiDocs` |
  | `service/` | `XxxService` |
  | `repository/` | `XxxRepository` |
  | `entity/` | 엔티티 |
  | `converter/` | `XxxConverter` (요청 DTO → Entity, Entity → 응답 DTO) |
  | `dto/` | `XxxCreateRequest`, `XxxUpdateRequest`, `XxxResponse` 등 record |

- 도메인명은 영어 소문자 (예: `user`, `chore`). `global/` 에는 둘 이상의 도메인이 쓰는 것만 둔다.

```
com/cmc/hackathon4/
├── domain/{도메인}/
│   ├── controller/   XxxController, XxxApiDocs
│   ├── service/      XxxService
│   ├── repository/   XxxRepository
│   ├── entity/       Xxx
│   ├── converter/    XxxConverter
│   └── dto/          XxxCreateRequest, XxxUpdateRequest, XxxResponse
└── global/           config/, entity/, exception/, response/
```

## 이름
- `XxxController`, `XxxApiDocs`, `XxxService`(인터페이스 분리 X), `XxxRepository`, `XxxCreateRequest`/`XxxUpdateRequest`/`XxxResponse`.
- URL: `/api/v1/{복수형-kebab}`. 응답은 항상 `ApiResponse.ok(...)`.

## DTO·Entity
- DTO 는 `record`. Entity 를 응답으로 반환 X.
- 변환은 `converter/XxxConverter` (static 메서드만 있는 `final` 클래스, private 생성자): `toEntity(요청 DTO)`, `toResponse(entity)`. DTO·Entity 에 변환 메서드(`from` 등)를 두지 않는다. 기존 Entity 의 값 변경은 Entity 의 의도 드러나는 메서드로 한다.
- Entity: `BaseTimeEntity` 상속, `@Getter` + `@NoArgsConstructor(access = PROTECTED)`. `@Setter`/`@Data` 금지, 변경은 의도 드러나는 메서드로.
- `@ManyToOne`/`@OneToOne` 은 `LAZY`, 양방향은 꼭 필요할 때만, 목록 N+1 은 fetch join / `@EntityGraph`.
- enum 은 `EnumType.STRING`, 시간은 `Instant`(UTC), ID 는 `Long` + `IDENTITY`. 스키마는 `ddl-auto: update` (Flyway X).

## 서비스·예외·검증
- 서비스 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드만 `@Transactional`.
- 실패는 `throw new BusinessException(ErrorCode.XXX)`. 새 코드는 `USER_NOT_FOUND(NOT_FOUND, "USER-404", "...")` 형식.
- `@RequestBody` 에 `@Valid`, 제약은 요청 record 필드에 (메시지 한글).
- `@PathVariable`·`@RequestParam` 에 `@Pattern` 같은 제약을 컨트롤러에 달지 않는다. `XxxApiDocs` 인터페이스와 충돌(HV000151)해 그 컨트롤러 전체가 500 이 된다. 서비스에서 검사하고 `BusinessException(INVALID_INPUT, "필드: 메시지")` 로 던진다.

## Swagger·로그·테스트
- Swagger 어노테이션은 `XxxApiDocs` 인터페이스에만. 컨트롤러는 implements 하고 매핑·로직만.
- 에러 응답은 `@ApiResponse(responseCode = "4xx", ...)` 로 손으로 쓰지 않는다. `@ApiErrorCodes({ ErrorCode.XXX, ... })` 로 그 API 가 낼 수 있는 코드를 적으면 상태별 응답과 코드별 예시가 자동으로 만들어진다 (`global/swagger/`).
  - 그 API 에서만의 발생 상황은 `@ApiErrorCase(code = ErrorCode.XXX, when = "...")` 로 덧붙인다.
  - 요청 본문 검증 실패(`COMMON-400`)와 `@CurrentUser` 의 헤더 없음(`COMMON-400`)·미등록 기기(`USER-404`)는 자동으로 붙으므로 적지 않는다.
  - 성공 응답(`200` 등)은 지금처럼 `@ApiResponse` 로 쓴다.
- 비밀번호, 토큰, 개인정보, 요청 본문 전체는 로그 금지. BusinessException 은 로그 남기지 않음.
- 테스트는 서비스 핵심 로직만, 메서드명 한글 허용 (H2 로 실행).

## 변경 시 함께 수정할 곳
| 변경 | 함께 수정 |
| --- | --- |
| 새 도메인 | `domain/{도메인}/{controller,service,repository,entity,converter,dto}/` 클래스들, 필요하면 `ErrorCode` (`{도메인}-{상태}` 형식) |
| 새 에러 코드 / 서비스가 던지는 에러 변경 | `ErrorCode` + 그 에러를 낼 수 있는 API 의 `XxxApiDocs` `@ApiErrorCodes` (Swagger 전체 표는 자동) |
| 운영 환경변수 추가 | `application-prod.yml` + GitHub Secret `APP_ENV` + `docs/deploy.md` 의 APP_ENV 예시 |
| 로컬 환경변수 추가 | `application-local.yml` + `.env.example` + `docs/local-dev.md` |
| 인프라 구성요소 추가 (예: 캐시 서버) | `build.gradle`, `docker-compose.yml`, `application-local.yml`, `scripts/ec2-setup.sh`(+ 서버 재실행), `docs/`, ADR 추가 |
| 프론트 주소 추가 | `application.yml` 의 `app.cors.allowed-origin-patterns` |

문서 수정 후 `bash scripts/check-docs.sh` 로 문서와 코드가 맞는지 확인한다. PR 을 올리면 `PR Check` 워크플로가 같은 검사를 자동으로 돌린다.

## 스타일·Git
- 포맷은 Spotless 가 담당: `./gradlew spotlessApply`.
- 브랜치·커밋·PR 규칙은 `CONTRIBUTING.md` 를 따른다.
