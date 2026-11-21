package com.cmc.hackathon4.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 목록. 도메인이 늘어나면 아래에 이어서 추가한다.
 * 예: USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-404", "사용자를 찾을 수 없습니다.")
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON-400", "요청 값이 올바르지 않습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-404", "요청한 대상을 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-405", "지원하지 않는 HTTP 메서드입니다."),
	CONFLICT(HttpStatus.CONFLICT, "COMMON-409", "다른 데이터와 충돌하여 처리할 수 없습니다."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON-415", "지원하지 않는 Content-Type 입니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 오류가 발생했습니다."),

	// [TEST] 임시 테스트 API
	TEST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "TEST-ITEM-404", "테스트 아이템을 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

}
