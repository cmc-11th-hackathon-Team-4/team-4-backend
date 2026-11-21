#!/usr/bin/env bash
# EC2 최초 1회 세팅 스크립트 (Amazon Linux 2023 / Ubuntu 공용)
# Session Manager 로 접속해서 실행한다 (SSH 불필요):
#   sudo bash ec2-setup.sh                    # 도메인 없이: http://<서버 IP> (80)
#   sudo bash ec2-setup.sh api.example.com    # 도메인 지정: https://api.example.com (인증서 자동 발급)
#
# 하는 일:
#   1) Java 21, Redis(localhost:6379), AWS CLI 설치
#   2) 서비스 전용 유저(team4) + /opt/team4-backend 생성, systemd 서비스 등록
#   3) 배포 스크립트 /usr/local/bin/team4-deploy.sh 설치 (GitHub Actions 가 SSM 으로 호출)
#   4) Caddy 설치 + localhost:8080 리버스 프록시
#      (도메인이 없으면 HTTP :80, 있으면 Let's Encrypt HTTPS 자동 발급)
# 여러 번 실행해도 된다. 도메인을 나중에 붙이거나 바꿀 때는 도메인을 인자로 다시 실행하면 된다.
# 다시 실행해도 앱 서비스, app.jar, .env 는 건드리지 않는다.
set -euo pipefail

DOMAIN="${1:-}"
if [ "$(id -u)" -ne 0 ]; then
  echo "root 권한이 필요합니다: sudo bash ec2-setup.sh [도메인]" >&2
  exit 1
fi

SERVICE=team4-backend
APP_USER=team4
APP_DIR=/opt/team4-backend

case "$(uname -m)" in
  x86_64)  CADDY_ARCH=amd64; AWS_ARCH=x86_64 ;;
  aarch64) CADDY_ARCH=arm64; AWS_ARCH=aarch64 ;;
  *) echo "지원하지 않는 CPU 아키텍처: $(uname -m)" >&2; exit 1 ;;
esac

# ---------- 1) 패키지: Java 21 + curl/unzip ----------
if command -v dnf >/dev/null 2>&1; then
  dnf install -y java-21-amazon-corretto-headless unzip
  command -v curl >/dev/null 2>&1 || dnf install -y curl-minimal
elif command -v apt-get >/dev/null 2>&1; then
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -y
  apt-get install -y openjdk-21-jre-headless curl unzip
else
  echo "지원하지 않는 OS 입니다 (dnf / apt 가 없음)" >&2
  exit 1
fi
java -version

# ---------- 1-1) Redis (같은 서버, localhost:6379 에서만 접근. 보안 그룹에 6379 를 열지 않는다) ----------
# 앱은 Spring 기본값(localhost:6379)으로 붙는다. 기본 설정이 127.0.0.1 바인딩 + protected-mode 라 외부 노출 없음
if command -v dnf >/dev/null 2>&1; then
  dnf install -y redis6
  REDIS_SERVICE=redis6
else
  apt-get install -y redis-server
  REDIS_SERVICE=redis-server
fi
systemctl enable --now "$REDIS_SERVICE"

# AWS CLI v2 (Amazon Linux 2023 은 기본 설치, Ubuntu 는 공식 설치 파일로 설치)
export PATH="/usr/local/bin:$PATH"
if ! command -v aws >/dev/null 2>&1; then
  TMP="$(mktemp -d)"
  curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-${AWS_ARCH}.zip" -o "$TMP/awscliv2.zip"
  unzip -q "$TMP/awscliv2.zip" -d "$TMP"
  "$TMP/aws/install"
  rm -rf "$TMP"
fi
aws --version

# ---------- 2) 앱 유저 / 디렉터리 / systemd 서비스 ----------
id "$APP_USER" >/dev/null 2>&1 || useradd --system --shell /usr/sbin/nologin "$APP_USER"
mkdir -p "$APP_DIR"
chown "$APP_USER": "$APP_DIR"

cat > "/etc/systemd/system/${SERVICE}.service" <<EOF
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

# ---------- 3) 배포 스크립트 (SSM 이 root 로 실행한다) ----------
cat > /usr/local/bin/team4-deploy.sh <<'EOF'
#!/usr/bin/env bash
# 사용법: team4-deploy.sh <s3-bucket> [region]
# S3 의 deploy/backend.jar, deploy/.env 를 받아 서비스를 재시작하고 헬스 체크한다.
set -euo pipefail
export PATH="/usr/local/bin:$PATH"

BUCKET="${1:?usage: team4-deploy.sh <s3-bucket> [region]}"
if [ -n "${2:-}" ]; then export AWS_DEFAULT_REGION="$2"; fi

SERVICE=team4-backend
APP_USER=team4
APP_DIR=/opt/team4-backend

aws s3 cp "s3://$BUCKET/deploy/backend.jar" "$APP_DIR/app.jar.new" --only-show-errors
chown "$APP_USER": "$APP_DIR/app.jar.new"
mv "$APP_DIR/app.jar.new" "$APP_DIR/app.jar"

# .env 는 있을 때만 갱신한다 (내용은 출력하지 않는다)
if (umask 077 && aws s3 cp "s3://$BUCKET/deploy/.env" "$APP_DIR/.env.new" --only-show-errors); then
  chown "$APP_USER": "$APP_DIR/.env.new"
  chmod 600 "$APP_DIR/.env.new"
  mv "$APP_DIR/.env.new" "$APP_DIR/.env"
else
  rm -f "$APP_DIR/.env.new"
  echo "warn: s3://<bucket>/deploy/.env 를 받지 못했다. 기존 .env 를 그대로 쓴다"
fi

systemctl restart "$SERVICE"

for i in $(seq 1 30); do
  if curl -fsS http://localhost:8080/api/v1/health >/dev/null 2>&1; then
    echo "deploy ok (${i} tries)"
    exit 0
  fi
  sleep 3
done

echo "health check failed"
systemctl is-active "$SERVICE" || true
journalctl -u "$SERVICE" -n 80 --no-pager
exit 1
EOF
chmod 755 /usr/local/bin/team4-deploy.sh

# ---------- 4) Caddy (공식 정적 바이너리 + systemd) ----------
# AL2023 기본 저장소에는 caddy 패키지가 없어서 두 OS 모두 공식 다운로드 API 로 받는다.
if ! command -v caddy >/dev/null 2>&1; then
  curl -fsSL "https://caddyserver.com/api/download?os=linux&arch=${CADDY_ARCH}" -o /usr/local/bin/caddy
  chmod 755 /usr/local/bin/caddy
fi
caddy version

getent group caddy >/dev/null || groupadd --system caddy
id caddy >/dev/null 2>&1 || useradd --system --gid caddy --create-home --home-dir /var/lib/caddy \
  --shell /usr/sbin/nologin caddy

# 도메인이 없으면 :80 (HTTP 만, 인증서 발급 시도 없음), 있으면 도메인 (HTTPS 자동)
SITE="${DOMAIN:-:80}"
mkdir -p /etc/caddy
cat > /etc/caddy/Caddyfile <<EOF
${SITE} {
	reverse_proxy localhost:8080
}
EOF

# 공식 배포 유닛(caddyserver/dist 의 init/caddy.service)을 줄인 것
cat > /etc/systemd/system/caddy.service <<EOF
[Unit]
Description=Caddy
After=network-online.target
Wants=network-online.target

[Service]
Type=notify
User=caddy
Group=caddy
ExecStart=$(command -v caddy) run --environ --config /etc/caddy/Caddyfile
ExecReload=$(command -v caddy) reload --config /etc/caddy/Caddyfile --force
TimeoutStopSec=5s
LimitNOFILE=1048576
PrivateTmp=true
ProtectSystem=full
AmbientCapabilities=CAP_NET_BIND_SERVICE
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable "$SERVICE"
systemctl enable caddy
systemctl restart caddy

echo
echo "완료"
echo "  Redis     : ${REDIS_SERVICE}.service (localhost:6379, 외부 비공개)"
echo "  앱 서비스 : ${SERVICE}.service (user=${APP_USER}, dir=${APP_DIR}) - app.jar 가 없어 아직 시작하지 않음"
if [ -n "$DOMAIN" ]; then
  echo "  Caddy     : https://${DOMAIN} -> localhost:8080 (인증서는 DNS 가 이 서버를 가리키면 자동 발급)"
else
  echo "  Caddy     : http://<서버 IP> (80) -> localhost:8080"
  echo "  ※ HTTPS 는 아직 꺼져 있습니다. 도메인의 A 레코드를 이 서버 IP 로 지정한 뒤 아래 명령을 다시 실행하세요:"
  echo "      sudo bash ec2-setup.sh <도메인>"
fi
echo "  배포      : main 에 push 하면 GitHub Actions 가 /usr/local/bin/team4-deploy.sh 를 실행"
echo "  로그      : journalctl -u ${SERVICE} -f / journalctl -u caddy -f"
