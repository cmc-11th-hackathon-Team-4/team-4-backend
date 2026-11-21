# 설계 결정 기록 (ADR)

"왜 이렇게 했는가" 를 남긴다. 결정이 바뀌면 기존 파일을 지우지 말고 상태를 `대체됨` 으로 바꾸고 새 번호로 추가한다.

| 번호 | 결정 | 상태 |
| --- | --- | --- |
| [0001](0001-ssm-instead-of-ssh.md) | 서버 접속·배포는 SSH(pem) 대신 SSM | 채택 |
| [0002](0002-caddy-duckdns-https.md) | HTTPS 는 Caddy + DuckDNS | 채택 |
| [0003](0003-redis-on-ec2.md) | Redis 는 ElastiCache 대신 EC2 에 직접 설치 | 대체됨(→ 0010) |
| [0010](0010-remove-redis.md) | Redis 를 쓰지 않아 제거 | 채택 |
| [0004](0004-no-elastic-ip.md) | 탄력적 IP 없이 자동 할당 공인 IP 사용 | 채택 (임시) |
| [0005](0005-ddl-auto-update.md) | 스키마는 Flyway 없이 `ddl-auto: update` | 채택 |
| [0006](0006-cors-frontend-only.md) | CORS 는 프론트 주소만 허용 | 채택 |
| [0007](0007-domain-then-layer-packages.md) | 패키지는 도메인 먼저, 그 안에서 계층별로 | 채택 |
| [0008](0008-converter-classes.md) | DTO↔Entity 변환은 static Converter 클래스로 | 채택 |
| [0009](0009-device-id-sha256.md) | deviceId 는 SHA-256 해시로 저장 | 채택 |

## 새 ADR 쓰는 법

`NNNN-짧은-제목.md` 로 만들고 아래 형식을 따른다. 10~20 줄이면 충분하다.

```
# NNNN. 제목
- 상태: 채택 | 대체됨(→ NNNN) | 폐기
- 날짜: YYYY-MM-DD
## 배경
## 결정
## 결과 (장단점, 따라오는 제약)
```
