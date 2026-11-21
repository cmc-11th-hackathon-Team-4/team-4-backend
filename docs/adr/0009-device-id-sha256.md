# 0009. deviceId 는 SHA-256 해시로 저장

- 상태: 채택
- 날짜: 2026-11-22

## 배경

로그인이 없어 앱이 만든 기기 UUID(deviceId)를 `X-Device-Id` 헤더로 받아 사용자를 식별한다.
이 값만 알면 그 사용자로 요청할 수 있는데 `users.device_id` 에 평문으로 저장하고 있었다. DB 내용이 보이면 누구로든 요청할 수 있다.

## 결정

- `users.device_id` 에는 deviceId 의 SHA-256 해시(소문자 hex 64자)만 저장한다. 컬럼 이름·길이는 그대로 둔다.
- 해시는 `User` 생성자와 `UserRepository.findByDeviceId` / `existsByDeviceId` 안에서 한다. 호출하는 쪽은 원본 deviceId 를 그대로 넘긴다.
- bcrypt 는 쓰지 않는다. bcrypt 는 값마다 salt 가 달라 해시값으로 사용자를 조회할 수 없고, 요청마다 느린 연산이 든다.
  느린 해시는 짧은 비밀번호의 대입 공격을 막기 위한 것인데 deviceId 는 무작위 UUID 라 대입 대상이 아니다.

## 결과

- 프론트가 보내는 값(헤더, SSE 쿼리, 프로필 만들기 본문)은 바뀌지 않는다.
- DB 에서 deviceId 원본을 알 수 없다. 특정 사용자로 요청하려면 그 기기가 가진 값을 써야 한다.
- 이 변경 전에 평문으로 저장된 행은 한 번 바꿔야 한다: `UPDATE users SET device_id = SHA2(device_id, 256) WHERE CHAR_LENGTH(device_id) <> 64;`
- 만료가 없는 고정 값이라는 점은 그대로다. 실제 로그인을 붙이면 다시 결정한다.
