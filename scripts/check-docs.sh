#!/usr/bin/env bash
# 문서와 코드가 어긋나지 않았는지 검사한다. 저장소 루트에서 실행: bash scripts/check-docs.sh
#  1) application-prod.yml 의 ${VAR} 가 docs/deploy.md 에 있는가 (APP_ENV 안내)
#  2) application-local.yml 의 ${VAR} 가 .env.example 에 있는가
#  3) docker-compose.yml 의 호스트 포트가 docs/local-dev.md 에 있는가
#  4) .github/workflows/*.yml 이 문서 어딘가에 언급되는가
#  5) 문서의 상대 링크 [..](경로) 와 `저장소 경로` 가 실제로 존재하는가
# 실패하면 exit 1. macOS 기본 bash(3.2)에서도 돌도록 배열·연관배열을 쓰지 않는다.
set -u
cd "$(dirname "$0")/.." || exit 1

FAIL=0
fail() { echo "  ✗ $1"; FAIL=1; }

DOCS=$(find . -name '*.md' -not -path './build/*' -not -path './.gradle/*' -not -path './node_modules/*' | sort)

env_vars() { grep -o '\${[A-Z_][A-Z0-9_]*' "$1" | sed 's/\${//' | sort -u; }

echo "[1] 운영 환경변수 → docs/deploy.md"
for v in $(env_vars src/main/resources/application-prod.yml); do
  grep -q "\b$v\b" docs/deploy.md || fail "$v 가 docs/deploy.md 에 없다"
done

echo "[2] 로컬 환경변수 → .env.example"
for v in $(env_vars src/main/resources/application-local.yml); do
  grep -q "^$v=" .env.example || fail "$v 가 .env.example 에 없다"
done

echo "[3] docker-compose 호스트 포트 → docs/local-dev.md"
for p in $(grep -oE '"[0-9]+:[0-9]+"' docker-compose.yml | tr -d '"' | cut -d: -f1); do
  grep -q "$p" docs/local-dev.md || fail "포트 $p 가 docs/local-dev.md 에 없다"
done

echo "[4] 워크플로 → 문서 언급"
for w in .github/workflows/*.yml; do
  name=$(basename "$w")
  # shellcheck disable=SC2086
  grep -q "$name" $DOCS || fail "$name 이 어떤 문서에도 언급되지 않는다"
done

echo "[5] 문서 속 링크·경로 → 실제 파일"
for doc in $DOCS; do
  dir=$(dirname "$doc")
  # 상대 링크: [텍스트](경로) - http(s), mailto, #앵커만 있는 것은 제외
  for link in $(grep -oE '\]\([^)#[:space:]]+' "$doc" | sed 's/^](//' | grep -vE '^(https?:|mailto:)'); do
    [ -e "$dir/$link" ] || fail "$doc: 링크 대상 없음 → $link"
  done
  # 백틱 경로: 저장소 최상위 디렉터리로 시작하거나 확장자가 있는 것만. 자리표시자(<, {, *)는 제외
  for p in $(grep -oE '`(src|scripts|docs|\.github|gradle)/[A-Za-z0-9_./-]+`|`[A-Za-z0-9_.-]+\.(yml|md|sh|gradle|example)`' "$doc" | tr -d '`' | sort -u); do
    case "$p" in *'<'*|*'{'*|*'*'*) continue ;; esac
    [ -e "$p" ] || [ -e "$dir/$p" ] || [ -n "$(find . -path ./build -prune -o -name "$(basename "$p")" -print -quit)" ] \
      || fail "$doc: 경로 없음 → $p"
  done
done

if [ "$FAIL" -eq 0 ]; then
  echo "OK: 문서와 코드가 일치한다"
else
  echo "FAIL: 위 항목을 고친다"
  exit 1
fi
