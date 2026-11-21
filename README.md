# team-4-backend

Spring Boot 4.1.3 / Java 21 백엔드.

## 배포 (CI/CD)

`main` 에 push 하면 GitHub Actions(`.github/workflows/deploy.yml`)가 jar 를 빌드해서 EC2 한 대에 올리고 systemd 서비스를 재시작한다.

### 최초 세팅

1. **EC2 생성**: Amazon Linux 2023 또는 Ubuntu. 보안 그룹 인바운드에 `22`(SSH), `8080`(앱) 을 연다.
   RDS 를 쓴다면 RDS 보안 그룹에서 이 EC2 의 `3306` 접근도 허용한다.
2. **서버 1회 세팅**: `scripts/ec2-setup.sh` 를 서버에 올려 실행한다.

   ```bash
   scp -i <키.pem> scripts/ec2-setup.sh <유저>@<EC2 주소>:~
   ssh -i <키.pem> <유저>@<EC2 주소> "bash ec2-setup.sh"
   ```

3. **GitHub Secrets 등록** (Settings → Secrets and variables → Actions)

   | 이름 | 값 |
   | --- | --- |
   | `EC2_HOST` | EC2 퍼블릭 IP 또는 DNS |
   | `EC2_USER` | `ec2-user` (Amazon Linux) / `ubuntu` (Ubuntu) |
   | `EC2_SSH_KEY` | `.pem` 키 파일 내용 전체 (`-----BEGIN` 부터 `-----END` 줄까지) |
   | `APP_ENV` | 아래 형식의 환경변수 여러 줄. 서버의 `~/app/.env` 로 저장된다 |

   `APP_ENV` 예시 (`application-prod.yml` 이 읽는 값):

   ```
   DB_HOST=<RDS 엔드포인트>
   DB_PORT=3306
   DB_NAME=<DB 이름>
   DB_USERNAME=<DB 유저>
   DB_PASSWORD=<DB 비밀번호>
   ```

   `export` 나 따옴표 없이 `KEY=value` 로만 적는다. `DB_PORT` 는 생략하면 3306.

4. **`main` 에 push** → Actions 탭에서 배포 확인. 수동 실행은 Actions → Deploy to EC2 → Run workflow.

### 확인 / 디버깅 (EC2 에서)

```bash
sudo journalctl -u team4-backend -f        # 실시간 로그
sudo journalctl -u team4-backend -n 200    # 최근 로그
sudo systemctl status team4-backend        # 상태
sudo systemctl restart team4-backend       # 재시작
curl localhost:8080/api/v1/health          # 헬스 체크
```

- 테스트는 배포 속도를 위해 CI 에서 건너뛴다 (`-x test`).
- EC2 를 재시작해 퍼블릭 IP 가 바뀌면 `EC2_HOST` 시크릿을 다시 등록한다 (탄력적 IP 를 붙이면 고정).
