# 🤝 협업 컨벤션

## 🌿 브랜치

| 브랜치 | 용도 |
| --- | --- |
| `main` | 배포 / 최종본 |
| `feat/기능명` | 기능 개발 (예: `feat/login`) |
| `fix/버그명` | 버그 수정 (예: `fix/login-error`) |
| `refactor/내용` | 코드 개선 (예: `refactor/user-service`) |
| `chore/내용` | 설정, 빌드 (예: `chore/cicd`) |

- `main`에서 브랜치 따서 작업 → PR → `main`에 머지
- **`main`에 직접 push 금지**
- 브랜치명은 영어 소문자 + `-` 로 (한글 브랜치명은 터미널/CI에서 깨질 수 있음)

## 📝 커밋 메시지

```
타입: 작업 내용
```

| 타입 | 의미 |
| --- | --- |
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 코드 개선 (기능 변화 없음) |
| `docs` | 문서 수정 |
| `chore` | 설정, 빌드, 기타 |
| `test` | 테스트 코드 추가/수정 |

예시: `feat: 로그인 API 추가`, `fix: 회원가입 오류 수정`

## 🔀 PR

- 제목은 커밋 메시지와 같은 형식 (`feat: 로그인 기능`)
- 본문은 PR 템플릿에 맞춰 간단히 작성
- 팀원 1명 확인 후 머지 (급하면 셀프 머지 OK)
- 머지 방식은 **Squash and merge** 권장 → `main` 히스토리가 PR 단위로 깔끔하게 남음
- 머지 후 브랜치 삭제
- PR 을 올리면 **PR Check**(문서와 코드 일치 검사)가 자동으로 돈다. 실패하면 Actions 로그에 고칠 항목이 나온다

## ⚡ 작업 순서

```bash
git checkout main
git pull origin main
git checkout -b feat/기능명
# 작업 후
git add .
git commit -m "feat: 작업 내용"
git push origin feat/기능명
# GitHub에서 PR 생성
```

### 작업 중에 main이 바뀌었다면

```bash
git checkout main
git pull origin main
git checkout feat/기능명
git merge main        # 충돌 나면 해결 후 커밋
git push origin feat/기능명
```

## 🔒 주의

- `.env`, `*.pem`, 키/비밀번호는 절대 커밋하지 않기 (`.gitignore`에 포함됨)
- 실수로 올렸다면 바로 팀에 공유하고 키를 재발급하기
