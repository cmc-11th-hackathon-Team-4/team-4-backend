# 운영 배포 런북

## 현재 운영 구성 (2026-11-22 기준)

| 항목 | 값 |
| --- | --- |
| 리전 | `ap-northeast-2` (서울) |
| 서버 | EC2 1대, Ubuntu |
| 공개 주소 | `https://niturnnaeturn.duckdns.org` (DuckDNS → 서버 IP, Caddy 가 Let's Encrypt 인증서 자동 발급·갱신) |
| 서버 IP | 자동 할당 공인 IP (**탄력적 IP 아님**). 인스턴스를 *중지 → 시작* 하면 IP 가 바뀐다 → DuckDNS 의 IP 도 바꿔야 한다. 재부팅(reboot)은 괜찮다 ([ADR 0004](adr/0004-no-elastic-ip.md)) |
| DB | RDS MySQL. 데이터베이스는 앱이 처음 뜰 때 자동 생성 (`createDatabaseIfNotExist=true`) |
| Redis | EC2 안에 설치 (`redis-server`, `localhost:6379` 전용, 외부 비공개) ([ADR 0003](adr/0003-redis-on-ec2.md)) |
| 서버 접속 | Session Manager (SSM). 배포도 SSM 으로 한다 ([ADR 0001](adr/0001-ssm-instead-of-ssh.md)) |
| 보안 그룹 인바운드 | `80`, `443` (0.0.0.0/0). `22` 도 현재 열려 있다 (팀 결정으로 유지, 배포에는 쓰지 않음) |

## 배포 흐름

```
main 에 머지(push) → .github/workflows/deploy.yml
  1. ./gradlew clean bootJar -x test   (CI 환경이라 Spotless 는 검사만, 테스트는 건너뜀)
  2. backend.jar 와 APP_ENV 를 S3 deploy/ 에 업로드
  3. SSM 으로 서버의 /usr/local/bin/team4-deploy.sh 실행
     → S3 에서 jar/.env 받기 → systemd 재시작 → localhost:8080/api/v1/health 최대 90초 확인
```

- 수동 실행: Actions → **Deploy to EC2** → Run workflow
- 배포 확인: `curl https://niturnnaeturn.duckdns.org/api/v1/health`
- Actions 로그에 SSM 명령의 stdout/stderr 가 찍힌다. 헬스 체크 실패 시 앱 로그 마지막 80 줄이 포함된다.

### 알려진 한계

- **롤백 경로가 없다.** 배포 시 `app.jar` 를 덮어써서 이전 jar 가 남지 않는다. 되돌리려면 이전 커밋을 되돌리는(revert) PR 을 머지해 다시 배포한다.
- 서버 1대라 재시작하는 동안(수 초~수십 초) 요청이 실패한다.
- CI 에서 테스트를 돌리지 않는다 (`-x test`). PR 단계 CI 도 아직 없다.
- 스키마는 `ddl-auto: update` 라 컬럼 이름 변경·삭제는 반영되지 않는다 ([ADR 0005](adr/0005-ddl-auto-update.md)).

## 처음부터 다시 구축할 때

### 1. AWS 준비 (콘솔, 순서대로)

1. **S3 버킷 생성**: 이름만 정하고 나머지는 기본값(퍼블릭 액세스 전부 차단). `.env` 가 올라가므로 공개하면 안 된다.
2. **EC2 용 IAM 역할 생성**: 신뢰 대상 EC2, 관리형 정책 `AmazonSSMManagedInstanceCore` 연결 + 아래 인라인 정책 추가.

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": "s3:GetObject",
         "Resource": "arn:aws:s3:::<BUCKET>/deploy/*"
       }
     ]
   }
   ```

3. **EC2 생성**: Ubuntu 또는 Amazon Linux 2023. 키 페어는 "키 페어 없이 진행". 고급 세부 정보의 IAM 인스턴스 프로파일에 2 의 역할 지정.
   보안 그룹 인바운드는 `80`, `443` (0.0.0.0/0). `8080`, `6379` 는 열지 않는다.
   RDS 보안 그룹에는 이 EC2 보안 그룹을 소스로 `3306` 을 허용한다.
4. **(권장) 탄력적 IP 할당 후 인스턴스에 연결**: 중지·시작해도 IP 가 바뀌지 않는다. 현재 운영은 쓰지 않고 있다 ([ADR 0004](adr/0004-no-elastic-ip.md)).
5. **GitHub Actions 용 IAM 사용자 생성**: 콘솔 액세스 없이 만들고 아래 인라인 정책만 붙인 뒤 액세스 키를 발급한다.

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": "s3:PutObject",
         "Resource": "arn:aws:s3:::<BUCKET>/deploy/*"
       },
       {
         "Effect": "Allow",
         "Action": "ssm:SendCommand",
         "Resource": [
           "arn:aws:ec2:<REGION>:<ACCOUNT_ID>:instance/<INSTANCE_ID>",
           "arn:aws:ssm:<REGION>::document/AWS-RunShellScript"
         ]
       },
       {
         "Effect": "Allow",
         "Action": "ssm:GetCommandInvocation",
         "Resource": "*"
       }
     ]
   }
   ```

### 2. 서버 1회 세팅 (`scripts/ec2-setup.sh`)

설치되는 것: Java 21, Redis(`localhost:6379`), AWS CLI, 앱 서비스(`team4-backend`, 유저 `team4`, 경로 `/opt/team4-backend`), 배포 스크립트(`/usr/local/bin/team4-deploy.sh`), Caddy.
여러 번 실행해도 안전하다. 다시 실행해도 앱 서비스, `app.jar`, `.env` 는 건드리지 않는다.

1. `scripts/ec2-setup.sh` 를 버킷의 `deploy/` 에 업로드한다 (비공개 저장소라 서버가 GitHub 에서 직접 받을 수 없다).
2. EC2 콘솔 → 인스턴스 → 연결 → **Session Manager** → 연결.
3. 실행한다.

   ```bash
   cd /tmp
   # 처음(Ubuntu 는 AWS CLI 가 아직 없음): S3 콘솔 → 객체 → 작업 → "미리 서명된 URL 로 공유" 로 만든 URL 사용
   curl -fsSL -o ec2-setup.sh '<미리 서명된 URL>'
   # 한 번 세팅한 뒤(AWS CLI 설치됨) 또는 Amazon Linux 2023:
   # aws s3 cp s3://<BUCKET>/deploy/ec2-setup.sh .

   sudo bash ec2-setup.sh              # 도메인 없이: http://<서버 IP> (80)
   sudo bash ec2-setup.sh <도메인>     # 도메인 있음: https://<도메인>
   ```

> 출력 마지막의 "app.jar 가 없어 아직 시작하지 않음" 문구는 상태와 관계없이 항상 찍힌다. 실제 상태는 `systemctl status team4-backend` 로 본다.

### 3. GitHub Secrets

Settings → Secrets and variables → Actions

| 이름 | 값 |
| --- | --- |
| `AWS_ACCESS_KEY_ID` | 1-5 에서 만든 IAM 사용자의 액세스 키 ID (`AKIA...`, ARN 아님) |
| `AWS_SECRET_ACCESS_KEY` | 위 사용자의 비밀 액세스 키 |
| `AWS_REGION` | `ap-northeast-2` |
| `S3_BUCKET` | 버킷 이름만 (`s3://`, 경로 없이) |
| `EC2_INSTANCE_ID` | 인스턴스 ID (`i-...`) |
| `APP_ENV` | 아래 형식의 여러 줄. 서버의 `/opt/team4-backend/.env` 로 저장된다 |

`APP_ENV` (`application-prod.yml` 이 읽는 값):

```
DB_HOST=<RDS 엔드포인트>
DB_PORT=3306
DB_NAME=<데이터베이스 이름. 없으면 앱이 자동 생성>
DB_USERNAME=<DB 유저>
DB_PASSWORD=<DB 비밀번호>
```

- `export`, 따옴표, `<>` 없이 `KEY=value` 로만 적는다. `DB_PORT` 는 생략하면 3306.
- `DB_NAME` 은 RDS **식별자가 아니라** 그 안의 데이터베이스(스키마) 이름이다.
- 데이터베이스 자동 생성에는 CREATE 권한이 있는 계정(마스터 계정 등)이 필요하다.
- Redis 는 Spring 기본값(`localhost:6379`)을 쓰므로 환경변수가 필요 없다.

### 4. 첫 배포

Actions → Deploy to EC2 → Run workflow (또는 `main` 에 머지).

## 도메인 + HTTPS

1. 도메인의 A 레코드를 서버 IP 로 지정한다. DuckDNS 는 <https://www.duckdns.org> 에서 IP 를 입력한다.
2. 아래 둘 중 하나로 서버에 적용한다.
   - **Actions (권장, 서버 접속 불필요)**: Actions → **Setup HTTPS (Caddy domain)** → Run workflow → 도메인 입력 (기본값 `niturnnaeturn.duckdns.org`).
     A 레코드 확인 → 최신 `ec2-setup.sh` 업로드 → SSM 으로 실행 → `https://<도메인>/api/v1/health` 확인까지 한다.
   - **Session Manager**: `sudo bash ec2-setup.sh <도메인>`

- 인증서 발급에는 DNS 가 서버를 가리키고 80/443 이 열려 있어야 한다.
- 적용 후 `http://<도메인>` 과 `http://<IP>` 는 https 로 리다이렉트된다. **IP 로는 더 이상 쓸 수 없다** (인증서가 도메인용). 프론트의 API 주소는 `https://<도메인>` 이어야 한다.
- 도메인을 사지 않고 HTTPS 를 쓰려면 DuckDNS 를 권장한다 (Public Suffix List 에 있어 Let's Encrypt 발급 한도를 서브도메인별로 받는다). sslip.io 는 발급 한도를 전 세계 사용자와 공유해서 임시용으로만 쓴다.

## 서버 구성 변경을 반영할 때

`scripts/ec2-setup.sh` 를 바꿨다면(예: Redis 추가) 서버에서 다시 실행해야 반영된다. 배포(`deploy.yml`)는 스크립트를 다시 실행하지 않는다.
**Setup HTTPS 워크플로를 같은 도메인으로 실행**하면 `main` 의 최신 `ec2-setup.sh` 가 서버에 적용된다.

## 확인 / 디버깅 (Session Manager)

```bash
sudo journalctl -u team4-backend -f        # 앱 실시간 로그
sudo journalctl -u team4-backend -n 200    # 앱 최근 로그
sudo journalctl -u caddy -f                # Caddy 로그 (인증서 발급 실패 원인도 여기)
sudo systemctl status team4-backend caddy redis-server
sudo systemctl restart team4-backend
curl localhost:8080/api/v1/health          # 앱 직접 헬스 체크
redis-cli ping                             # Redis (PONG)
cat /etc/caddy/Caddyfile                   # 현재 적용된 도메인
```
