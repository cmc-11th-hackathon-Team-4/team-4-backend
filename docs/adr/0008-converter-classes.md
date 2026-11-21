# 0008. DTO↔Entity 변환은 static Converter 클래스로

- 상태: 채택 (기존 "변환은 `XxxResponse.from(entity)`, Converter 클래스 X" 규칙을 대체)
- 날짜: 2026-11-22

## 배경

PR #3 의 AGENTS.md 는 변환을 응답 DTO 의 `from(entity)` 로 하고 Converter 클래스를 쓰지 않도록 정했다. 그 이유는 기록되지 않았다.
이 방식에서는 DTO 가 Entity 를 알아야 하고(dto → entity 의존), 요청 → Entity 생성은 서비스에 `new Xxx(...)` 로 흩어진다.

## 결정

- 도메인마다 `converter/XxxConverter` 를 둔다. static 메서드만 있는 `final` 클래스(private 생성자), 스프링 빈 아님.
- `toEntity(요청 DTO)`: 새 Entity 생성. `toResponse(entity)`: 응답 DTO 생성.
- DTO·Entity 에는 변환 메서드를 두지 않는다. 기존 Entity 의 값 변경은 Entity 의 의도 드러나는 메서드(`update(...)` 등)로 한다.
- Entity 가 없는 도메인(`health`, `testredis`)에는 만들지 않는다.

## 결과

- 변환 로직이 도메인당 한 곳에 모이고, DTO 는 Entity 를 몰라도 된다.
- 도메인마다 클래스가 하나 늘어난다.
- static 이라 다른 빈(예: 파일 URL 생성기)이 필요한 변환은 이 방식으로 할 수 없다. 그런 변환이 생기면 그 Converter 만 빈으로 바꿀지 다시 결정한다.
