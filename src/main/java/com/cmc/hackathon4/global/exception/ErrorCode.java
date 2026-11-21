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

	// 사용자
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-404", "사용자를 찾을 수 없습니다."),
	USER_ALREADY_REGISTERED(HttpStatus.CONFLICT, "USER-409", "이미 프로필이 등록된 기기입니다."),

	// 집
	HOUSE_NOT_FOUND(HttpStatus.NOT_FOUND, "HOUSE-404", "집을 찾을 수 없습니다."),
	HOUSE_INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "HOUSE-404-1", "초대 코드가 올바르지 않습니다."),
	HOUSE_OWNER_ONLY(HttpStatus.FORBIDDEN, "HOUSE-403", "방장만 할 수 있습니다."),
	HOUSE_NOT_MEMBER(HttpStatus.FORBIDDEN, "HOUSE-403-1", "이 집의 구성원이 아닙니다."),
	HOUSE_ALREADY_JOINED(HttpStatus.CONFLICT, "HOUSE-409", "이미 참여 중인 집이 있습니다."),
	HOUSE_FULL(HttpStatus.CONFLICT, "HOUSE-409-1", "정원(5명)이 가득 찼습니다."),
	HOUSE_DISTRIBUTING(HttpStatus.CONFLICT, "HOUSE-409-2", "배분이 진행 중이라 지금은 할 수 없습니다."),
	HOUSE_NOT_DISTRIBUTING(HttpStatus.CONFLICT, "HOUSE-409-3", "배분 확인 단계가 아닙니다."),
	HOUSE_ALREADY_STARTED(HttpStatus.CONFLICT, "HOUSE-409-4", "이미 시작된 집입니다."),

	// 집안일
	CHORE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHORE-404", "집안일을 찾을 수 없습니다."),
	CHORE_DUPLICATED(HttpStatus.CONFLICT, "CHORE-409", "이미 있는 집안일입니다."),

	// 기간
	PERIOD_NOT_FOUND(HttpStatus.NOT_FOUND, "PERIOD-404", "진행 중인 기간이 없습니다."),
	PERIOD_NEXT_NOT_READY(HttpStatus.NOT_FOUND, "PERIOD-404-1", "준비된 다음 기간이 없습니다."),
	PERIOD_ENDED(HttpStatus.CONFLICT, "PERIOD-409", "이미 종료된 기간입니다."),
	PERIOD_NOT_ENDED(HttpStatus.CONFLICT, "PERIOD-409-1", "아직 종료되지 않은 기간입니다."),
	PERIOD_NEXT_ALREADY_STARTED(HttpStatus.CONFLICT, "PERIOD-409-2", "이미 다음 기간이 시작되었습니다."),

	// 짐
	ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ASSIGNMENT-404", "짐을 찾을 수 없습니다."),
	ASSIGNMENT_NOT_MINE(HttpStatus.FORBIDDEN, "ASSIGNMENT-403", "내 짐이 아닙니다."),
	ASSIGNMENT_OWN_RELIEVE(HttpStatus.BAD_REQUEST, "ASSIGNMENT-400", "내 짐은 덜어갈 수 없습니다."),
	ASSIGNMENT_ALREADY_DONE(HttpStatus.CONFLICT, "ASSIGNMENT-409", "이미 완료된 짐입니다."),
	ASSIGNMENT_REQUEST_PENDING(HttpStatus.CONFLICT, "ASSIGNMENT-409-1", "이미 부탁 중인 짐입니다."),

	// 짐 이동
	TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRANSFER-404", "요청을 찾을 수 없습니다."),
	TRANSFER_NOT_MINE(HttpStatus.FORBIDDEN, "TRANSFER-403", "내가 받은 요청이 아닙니다."),
	TRANSFER_ALREADY_HANDLED(HttpStatus.CONFLICT, "TRANSFER-409", "이미 처리된 요청입니다."),

	// 알림
	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION-404", "알림을 찾을 수 없습니다."),
	NOTIFICATION_NOT_MINE(HttpStatus.FORBIDDEN, "NOTIFICATION-403", "내 알림이 아닙니다."),

	// [TEST] 임시 테스트 API
	TEST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "TEST-ITEM-404", "테스트 아이템을 찾을 수 없습니다."),
	TEST_REDIS_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "TEST-REDIS-404", "Redis 키가 없거나 만료되었습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

}
