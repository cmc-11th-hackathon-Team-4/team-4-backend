package com.cmc.hackathon4.domain.notification.controller;

import com.cmc.hackathon4.domain.notification.dto.NotificationListResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Notification", description = "알림 API")
public interface NotificationApiDocs {

	@Operation(summary = "알림 읽음 처리(배너 닫기)", description = "하트 없이 알림을 확인하고 닫는다. 알림을 받은 사람만 할 수 있고, 이미 읽은 알림이면 그대로 성공한다. 응답 본문 없음.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내 알림이 아님 (NOTIFICATION-403)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림 없음 (NOTIFICATION-404) / 등록되지 않은 기기 (USER-404)")
	ApiResponse<Void> markRead(User user, @Parameter(description = "알림 ID", example = "12") Long notificationId);

	@Operation(summary = "내 알림 목록 조회", description = "내 알림을 최신순으로 최대 50개 내려준다. 기본은 안 읽은 알림만(unreadOnly=true). afterId 를 주면 그 id 보다 큰 알림만 조회한다(SSE 재연결 따라잡기용).")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "unreadOnly 가 boolean 이 아니거나 afterId 가 숫자가 아님 (COMMON-400)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록되지 않은 기기 (USER-404)")
	ApiResponse<NotificationListResponse> findMine(User user,
			@Parameter(description = "안 읽은 알림만 조회할지 (기본 true)", example = "true") boolean unreadOnly,
			@Parameter(description = "이 id 보다 큰 알림만 조회", example = "18") Long afterId);

}
