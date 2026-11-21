#!/usr/bin/env bash
# EC2 최초 1회 세팅 스크립트 (Amazon Linux 2023 / Ubuntu 공용)
# 사용법: EC2 에 로그인 사용자(ec2-user / ubuntu)로 접속해서  bash ec2-setup.sh
#
# 하는 일: Java 21 설치 -> ~/app 디렉터리 생성 -> systemd 서비스 등록(enable)
# 참고: GitHub Actions 가 `sudo systemctl restart` 를 쓴다. ec2-user / ubuntu 는
#       기본으로 비밀번호 없는 sudo 가 되므로 sudoers 설정은 따로 하지 않는다.
set -euo pipefail

SERVICE=team4-backend
APP_USER="${SUDO_USER:-$(id -un)}"
APP_DIR="$(getent passwd "$APP_USER" | cut -d: -f6)/app"

# 1) Java 21 (+ 헬스 체크용 curl)
if command -v dnf >/dev/null 2>&1; then
  sudo dnf install -y java-21-amazon-corretto-headless
  command -v curl >/dev/null 2>&1 || sudo dnf install -y curl-minimal
elif command -v apt-get >/dev/null 2>&1; then
  sudo apt-get update -y
  sudo apt-get install -y openjdk-21-jre-headless curl
else
  echo "지원하지 않는 OS 입니다 (dnf / apt 가 없음)" >&2
  exit 1
fi
java -version

# 2) 앱 디렉터리 (jar 와 .env 가 여기에 올라온다)
sudo -u "$APP_USER" mkdir -p "$APP_DIR"

# 3) systemd 서비스
sudo tee "/etc/systemd/system/${SERVICE}.service" >/dev/null <<EOF
[Unit]
Description=${SERVICE} (Spring Boot)
After=network-online.target
Wants=network-online.target

[Service]
User=${APP_USER}
WorkingDirectory=${APP_DIR}
EnvironmentFile=-${APP_DIR}/.env
ExecStart=$(command -v java) -jar ${APP_DIR}/app.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable "$SERVICE"

echo
echo "완료: ${SERVICE}.service 등록됨 (user=${APP_USER}, dir=${APP_DIR})"
echo "아직 app.jar 가 없으므로 서비스는 시작하지 않았습니다. main 에 push 하면 배포됩니다."
echo "로그 확인: sudo journalctl -u ${SERVICE} -f"
