# 0006. CORS 는 프론트 주소만 허용

- 상태: 채택
- 날짜: 2026-11-21 (PR #10)

## 배경

처음엔 `*` (전체 허용) 이었다. `WebConfig` 는 `allowCredentials(true)` 라서, 로그인이 생기면 아무 사이트에서나 사용자 인증 정보를 실어 API 를 호출할 수 있게 된다.

## 결정

`application.yml` 의 `app.cors.allowed-origin-patterns` 에 아래만 둔다.

- `https://team-4-frontend-bice.vercel.app` (Vercel 운영)
- `https://team-4-frontend-*.vercel.app` (Vercel 프리뷰)
- `http://localhost:5173` (로컬 Vite)

## 결과

- 목록에 없는 출처의 브라우저 요청은 403. 프론트 주소가 늘면 이 목록에 쉼표로 추가해야 한다.
- 브라우저가 아닌 요청(curl, 서버 간)과 같은 도메인의 Swagger 는 영향 없다.
- 프리뷰 패턴은 같은 이름 패턴의 다른 Vercel 프로젝트도 허용한다.
- 테스트는 `src/test/resources/application.yml` 에서 `*` 를 쓴다.
