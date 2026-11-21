# AGENTS.md

Spring Boot 4 / Java 21 / Lombok / JPA + MySQL. 기존 코드 패턴을 먼저 보고 따른다 (예시: `domain/health`, `global/`).

## 필수
- 작업 후 `./gradlew build` 통과 확인. `main` 은 push 즉시 운영 배포되므로 직접 커밋 금지.
- 요청 없이 수정 금지: `application-prod.yml`, `.github/`, `scripts/`, `docker-compose.yml`, `global/` 공통 클래스 (`ErrorCode` 추가는 허용).

## 구조·이름
- 도메인별 패키지: `domain/{도메인}/` 에 한 도메인 클래스를 모두 둔다. 공통은 `global/`.
- `XxxController`, `XxxApiDocs`, `XxxService`(인터페이스 분리 X), `XxxRepository`, `XxxCreateRequest`/`XxxUpdateRequest`/`XxxResponse`.
- URL: `/api/v1/{복수형-kebab}`. 응답은 항상 `ApiResponse.ok(...)`.

## DTO·Entity
- DTO 는 `record`. 변환은 `XxxResponse.from(entity)`, Converter 클래스 X. Entity 를 응답으로 반환 X.
- Entity: `BaseTimeEntity` 상속, `@Getter` + `@NoArgsConstructor(access = PROTECTED)`. `@Setter`/`@Data` 금지, 변경은 의도 드러나는 메서드로.
- `@ManyToOne`/`@OneToOne` 은 `LAZY`, 양방향은 꼭 필요할 때만, 목록 N+1 은 fetch join / `@EntityGraph`.
- enum 은 `EnumType.STRING`, 시간은 `Instant`(UTC), ID 는 `Long` + `IDENTITY`. 스키마는 `ddl-auto: update` (Flyway X).

## 서비스·예외·검증
- 서비스 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드만 `@Transactional`.
- 실패는 `throw new BusinessException(ErrorCode.XXX)`. 새 코드는 `USER_NOT_FOUND(NOT_FOUND, "USER-404", "...")` 형식.
- `@RequestBody` 에 `@Valid`, 제약은 요청 record 필드에 (메시지 한글).

## Swagger·로그·테스트
- Swagger 어노테이션은 `XxxApiDocs` 인터페이스에만. 컨트롤러는 implements 하고 매핑·로직만.
- 비밀번호, 토큰, 개인정보, 요청 본문 전체는 로그 금지. BusinessException 은 로그 남기지 않음.
- 테스트는 서비스 핵심 로직만, 메서드명 한글 허용 (H2 로 실행).

## 스타일·Git
- 포맷은 Spotless 가 담당: `./gradlew spotlessApply`.
- 브랜치·커밋·PR 규칙은 `CONTRIBUTING.md` 를 따른다.
