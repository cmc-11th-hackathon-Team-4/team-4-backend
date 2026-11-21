# 0002. HTTPS 는 Caddy + DuckDNS

- 상태: 채택
- 날짜: 2026-11-21 (PR #1 Caddy 도입, PR #9 HTTPS 전환 워크플로)

## 배경

프론트가 Vercel(HTTPS) 이라 API 가 HTTP 면 브라우저가 혼합 콘텐츠로 막는다. 유료 도메인은 없다.

## 결정

- 리버스 프록시는 Caddy. 도메인만 지정하면 Let's Encrypt 인증서를 자동 발급·갱신한다.
- 도메인은 DuckDNS 무료 서브도메인 (`niturnnaeturn.duckdns.org`).

## 결과

- 인증서 관리가 필요 없다. `setup-https.yml` 로 서버 접속 없이 전환·재적용할 수 있다.
- DuckDNS 는 Public Suffix List 에 있어 Let's Encrypt 발급 한도를 서브도메인별로 받는다 (sslip.io 는 한도를 공유해 임시용).
- 전환 후 IP 로는 접근할 수 없다 (https 리다이렉트 + 인증서가 도메인용).
- 서버 IP 가 바뀌면 DuckDNS 의 IP 를 직접 갱신해야 한다 ([0004](0004-no-elastic-ip.md)).
