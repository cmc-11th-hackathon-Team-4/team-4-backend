package com.cmc.hackathon4.domain.notification.controller;

import com.cmc.hackathon4.domain.notification.dto.NotificationListResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.exception.ErrorCode;
import com.cmc.hackathon4.global.response.ApiResponse;
import com.cmc.hackathon4.global.swagger.ApiErrorCase;
import com.cmc.hackathon4.global.swagger.ApiErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Notification", description = "알림 API")
public interface NotificationApiDocs {

	@Operation(summary = "알림 읽음 처리(배너 닫기)", description = "하트 없이 알림을 확인하고 닫는다. 알림을 받은 사람만 할 수 있고, 이미 읽은 알림이면 그대로 성공한다. 응답 본문 없음.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공")
	@ApiErrorCodes({ ErrorCode.NOTIFICATION_NOT_MINE, ErrorCode.NOTIFICATION_NOT_FOUND })
	ApiResponse<Void> markRead(User user, @Parameter(description = "알림 ID", example = "12") Long notificationId);

	@Operation(summary = "내 알림 목록 조회", description = "내 알림을 최신순으로 최대 50개 내려준다. 기본은 안 읽은 알림만(unreadOnly=true). afterId 를 주면 그 id 보다 큰 알림만 조회한다(SSE 재연결 따라잡기용).")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiErrorCase(code = ErrorCode.INVALID_INPUT, when = "unreadOnly 가 boolean 이 아니거나 afterId 가 숫자가 아님")
	ApiResponse<NotificationListResponse> findMine(User user,
			@Parameter(description = "안 읽은 알림만 조회할지 (기본 true)", example = "true") boolean unreadOnly,
			@Parameter(description = "이 id 보다 큰 알림만 조회", example = "18") Long afterId);

	@Operation(summary = "알림 실시간 스트림(SSE)", description = "text/event-stream. 응답 래퍼 없음. EventSource 가 헤더를 못 보내 X-Device-Id 대신 쿼리 deviceId 로 사용자를 식별한다. "
			+ "연결 직후 `event: connected`, 새 알림마다 `id`/`event: notification`/`data`(알림 JSON), 30초마다 `: ping` 주석. "
			+ "사용자당 연결 1개(새 연결이 오면 이전 연결 종료), 서버 타임아웃 30분. "
			+ "Last-Event-ID 헤더가 있으면 그 id 보다 큰 알림을 먼저 다시 보낸다. 에러는 연결 전에만 일반 JSON 으로 내려온다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "연결 성공 (text/event-stream)")
	@ApiErrorCase(code = ErrorCode.INVALID_INPUT, when = "deviceId 쿼리 없음")
	@ApiErrorCase(code = ErrorCode.USER_NOT_FOUND, when = "등록되지 않은 deviceId")
	SseEmitter stream(
			@Parameter(description = "앱이 발급해 보관하는 기기 UUID (X-Device-Id 와 같은 값)", example = "3f2b8c1e-7a4d-4e9b-9c1a-5d6e7f8a9b0c") String deviceId,
			@Parameter(hidden = true) String lastEventId);

}
