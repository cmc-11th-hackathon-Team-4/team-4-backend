package com.cmc.hackathon4.domain.notification.controller;

import com.cmc.hackathon4.domain.notification.dto.NotificationListResponse;
import com.cmc.hackathon4.domain.notification.service.NotificationService;
import com.cmc.hackathon4.domain.notification.service.NotificationStreamService;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.auth.CurrentUser;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import com.cmc.hackathon4.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController implements NotificationApiDocs {

	private final NotificationService notificationService;
	private final NotificationStreamService notificationStreamService;

	@Override
	@PatchMapping("/{notificationId}/read")
	public ApiResponse<Void> markRead(@CurrentUser User user, @PathVariable Long notificationId) {
		notificationService.markRead(user.getId(), notificationId);
		return ApiResponse.ok();
	}

	@Override
	@GetMapping
	public ApiResponse<NotificationListResponse> findMine(@CurrentUser User user,
			@RequestParam(defaultValue = "true") boolean unreadOnly, @RequestParam(required = false) Long afterId) {
		return ApiResponse.ok(new NotificationListResponse(notificationService.findMine(user.getId(), unreadOnly, afterId)));
	}

	// produces 를 지정하지 않는다: 연결 전 에러(JSON)가 Accept: text/event-stream 과 충돌하지 않게 하려는 것.
	// Content-Type 은 SseEmitter 가 text/event-stream 으로 정한다.
	@Override
	@GetMapping("/stream")
	public SseEmitter stream(@RequestParam(required = false) String deviceId,
			@RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {
		return notificationStreamService.subscribe(deviceId, lastEventId);
	}

	/**
	 * 연결 전 에러는 일반 JSON 으로 내려야 한다. EventSource 는 Accept: text/event-stream 을 보내는데,
	 * 전역 핸들러는 그 Accept 로는 JSON 을 쓸 수 없어(406) 예외가 처리되지 않으므로 Content-Type 을 JSON 으로 못 박는다.
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus())
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResponse.fail(errorCode, e.getMessage()));
	}

}
