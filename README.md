# team-4-backend

CMC 해커톤 4조 백엔드. Spring Boot 4.1.3 / Java 21 / JPA + MySQL / Redis.

| 항목 | 값 |
| --- | --- |
| 운영 API | `https://niturnnaeturn.duckdns.org` |
| Swagger | `https://niturnnaeturn.duckdns.org/swagger-ui/index.html` |
| 헬스 체크 | `GET /api/v1/health` |
| 프론트 (CORS 허용) | `https://team-4-frontend-bice.vercel.app`, Vercel 프리뷰 `https://team-4-frontend-*.vercel.app`, 로컬 `http://localhost:5173` |

## 빠른 시작 (로컬)

```bash
docker compose up -d          # MySQL(3307) + Redis(6380)
cp .env.example .env          # 선택: 없으면 기본값으로 동작
./gradlew bootRun             # http://localhost:8080/swagger-ui/index.html
```

자세한 내용은 [docs/local-dev.md](docs/local-dev.md).

## 배포 요약

`main` 에 머지(push)되면 **즉시 운영에 배포**된다 (`.github/workflows/deploy.yml`).

```
머지 → Actions: bootJar → S3(deploy/backend.jar, deploy/.env) → SSM send-command
     → EC2: /usr/local/bin/team4-deploy.sh (S3 에서 받기 → systemd 재시작 → 헬스 체크)
사용자 → Caddy (https://niturnnaeturn.duckdns.org :443) → localhost:8080 (Spring Boot) → RDS(MySQL) / Redis(localhost:6379)
```

서버 세팅, Secrets, HTTPS, 디버깅은 [docs/deploy.md](docs/deploy.md).

## 문서

| 문서 | 내용 |
| --- | --- |
| [AGENTS.md](AGENTS.md) | 코드 규칙 (사람·에이전트 공통). `CLAUDE.md` 는 이 파일을 불러온다 |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 브랜치·커밋·PR 규칙 |
| [docs/architecture.md](docs/architecture.md) | 구성 요소, 요청 흐름, 디렉터리 구조(도메인 분리), 설정 파일 역할 |
| [docs/local-dev.md](docs/local-dev.md) | 로컬 개발 환경 |
| [docs/deploy.md](docs/deploy.md) | 운영 배포·서버 세팅·HTTPS·디버깅 런북 |
| [docs/adr/](docs/adr/README.md) | 설계 결정 기록 (왜 그렇게 했는지) |

문서와 코드가 어긋나지 않았는지는 `bash scripts/check-docs.sh` 로 확인한다. PR 을 올리면 같은 검사가 자동으로 돈다 (`.github/workflows/pr-check.yml`).
