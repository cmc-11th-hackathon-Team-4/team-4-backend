# team-4-backend

Spring Boot 4.1.3 / Java 21 백엔드.

## 배포 (CI/CD)

`main` 에 push 하면 GitHub Actions(`.github/workflows/deploy.yml`)가 jar 를 빌드해 S3 에 올리고, SSM(Run Command)으로 EC2 에서 배포 스크립트를 실행한다. 외부에서는 Caddy(HTTPS)로만 접근한다.

```
push → Actions: bootJar → S3(deploy/backend.jar, deploy/.env) → SSM send-command
     → EC2: /usr/local/bin/team4-deploy.sh (S3 에서 받기 → systemd 재시작 → 헬스 체크)
사용자 → https://<도메인> (Caddy :443) → localhost:8080 (Spring Boot)
```

SSH 키와 22 번 포트는 쓰지 않는다. 보안 그룹 인바운드는 `80`, `443` 만 연다.

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

3. **EC2 생성**: Amazon Linux 2023 또는 Ubuntu. 키 페어는 "키 페어 없이 진행". 고급 세부 정보의 IAM 인스턴스 프로파일에 2 의 역할 지정.
   보안 그룹 인바운드는 `80`, `443` (0.0.0.0/0) 만. `22`, `8080` 은 열지 않는다.
   RDS 를 쓴다면 RDS 보안 그룹에서 이 EC2 보안 그룹의 `3306` 접근을 허용한다.
4. **탄력적 IP 할당 후 인스턴스에 연결**: 재시작해도 IP 가 바뀌지 않게 한다.
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

### 2. 도메인

도메인의 **A 레코드를 탄력적 IP 로** 지정한다 (예: `api.example.com`). Caddy 가 Let's Encrypt 인증서를 자동 발급하려면 DNS 가 먼저 이 서버를 가리키고 80/443 이 열려 있어야 한다.

도메인이 없다면:

- **DuckDNS** (권장): <https://www.duckdns.org> 에서 `<이름>.duckdns.org` 를 무료로 만들고 IP 에 탄력적 IP 를 입력한다. `duckdns.org` 는 Public Suffix List 에 등록돼 있어 Let's Encrypt 발급 한도를 서브도메인별로 따로 받는다.
- **sslip.io** (`<IP 를 대시로>.sslip.io`, 예: `1-2-3-4.sslip.io`): 가입 없이 바로 되지만 Public Suffix List 에 없어서 전 세계 sslip.io 사용자가 Let's Encrypt 주간 발급 한도(등록 도메인당 50 개)를 같이 쓴다. 한도에 걸려 발급이 실패할 수 있으므로 급할 때 임시용으로만 쓴다.

### 3. 서버 1회 세팅 (Session Manager)

1. `scripts/ec2-setup.sh` 를 S3 콘솔에서 버킷의 `deploy/` 폴더에 업로드한다 (비공개 저장소라 서버에서 바로 받을 수 없다).
2. EC2 콘솔 → 인스턴스 선택 → 연결 → **Session Manager** → 연결. (인스턴스 생성 직후에는 몇 분 걸릴 수 있다.)
3. 터미널에서 실행한다.

   ```bash
   cd /tmp
   # Amazon Linux 2023 (AWS CLI 기본 설치)
   aws s3 cp s3://<BUCKET>/deploy/ec2-setup.sh .
   # Ubuntu (AWS CLI 없음): S3 콘솔 → 객체 → 작업 → "미리 서명된 URL 로 공유" 로 만든 URL 사용
   # curl -fsSL -o ec2-setup.sh '<미리 서명된 URL>'

   sudo bash ec2-setup.sh <도메인>
   ```

   Java 21, AWS CLI, 앱 서비스(`team4-backend`, 유저 `team4`, 경로 `/opt/team4-backend`), 배포 스크립트, Caddy 가 설치된다. 도메인을 바꿀 때는 같은 명령을 새 도메인으로 다시 실행한다.

### 4. GitHub Secrets 등록

Settings → Secrets and variables → Actions

| 이름 | 값 |
| --- | --- |
| `AWS_ACCESS_KEY_ID` | 1-5 에서 만든 IAM 사용자의 액세스 키 ID |
| `AWS_SECRET_ACCESS_KEY` | 위 사용자의 시크릿 액세스 키 |
| `AWS_REGION` | 리전 (예: `ap-northeast-2`) |
| `S3_BUCKET` | 버킷 이름 (`s3://` 없이) |
| `EC2_INSTANCE_ID` | 인스턴스 ID (`i-...`) |
| `APP_ENV` | 아래 형식의 환경변수 여러 줄. 서버의 `/opt/team4-backend/.env` 로 저장된다 |

`APP_ENV` 예시 (`application-prod.yml` 이 읽는 값):

```
DB_HOST=<RDS 엔드포인트>
DB_PORT=3306
DB_NAME=<DB 이름>
DB_USERNAME=<DB 유저>
DB_PASSWORD=<DB 비밀번호>
```

`export` 나 따옴표 없이 `KEY=value` 로만 적는다. `DB_PORT` 는 생략하면 3306.

### 5. 배포

`main` 에 push → Actions 탭에서 확인. 수동 실행은 Actions → Deploy to EC2 → Run workflow.
배포 후 `https://<도메인>/api/v1/health` 로 확인한다.

### 확인 / 디버깅 (Session Manager 에서)

```bash
sudo journalctl -u team4-backend -f        # 앱 실시간 로그
sudo journalctl -u team4-backend -n 200    # 앱 최근 로그
sudo journalctl -u caddy -f                # Caddy 로그 (인증서 발급 실패 원인도 여기)
sudo systemctl status team4-backend caddy  # 상태
sudo systemctl restart team4-backend       # 재시작
curl localhost:8080/api/v1/health          # 앱 직접 헬스 체크
```

- 테스트는 배포 속도를 위해 CI 에서 건너뛴다 (`-x test`).
- Actions 로그에는 SSM 명령의 stdout/stderr 가 그대로 찍힌다. 헬스 체크 실패 시 앱 로그 마지막 80 줄이 포함된다.
