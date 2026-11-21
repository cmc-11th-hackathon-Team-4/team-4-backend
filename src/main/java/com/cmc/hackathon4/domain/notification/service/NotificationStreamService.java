package com.cmc.hackathon4.domain.notification.service;

import java.util.List;

import com.cmc.hackathon4.domain.notification.dto.NotificationResponse;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 알림 실시간 스트림(SSE) 연결을 연다. open-in-view 가 꺼져 있고 SSE 는 비동기 응답이라,
 * 필요한 조회(사용자, 놓친 알림)는 각 리포지토리·서비스 트랜잭션 안에서 끝내고 emitter 만 돌려준다.
 */
@Service
@RequiredArgsConstructor
public class NotificationStreamService {

	private final UserRepository userRepository;
	private final NotificationService notificationService;
	private final NotificationEmitterRegistry registry;

	/**
	 * @param deviceId EventSource 는 헤더를 못 보내므로 쿼리로 받은 기기 UUID
	 * @param lastEventId 재연결 때 브라우저가 보내는 Last-Event-ID 헤더 (없거나 숫자가 아니면 무시)
	 */
	public SseEmitter subscribe(String deviceId, String lastEventId) {
		if (!StringUtils.hasText(deviceId)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "필수 파라미터 'deviceId' 가 없습니다.");
		}
		Long userId = userRepository.findByDeviceId(deviceId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
				.getId();
		Long afterId = parseLastEventId(lastEventId);

		List<NotificationResponse> missed = afterId == null ? List.of()
				: notificationService.findMissed(userId, afterId);
		SseEmitter emitter = registry.register(userId);
		registry.sendConnected(userId, emitter);
		missed.forEach(notification -> registry.sendNotification(userId, emitter, notification));

		if (afterId != null) {
			// 위 조회와 등록 사이에 생긴 알림을 놓치지 않도록 한 번 더 따라잡는다.
			Long lastSent = missed.isEmpty() ? afterId : missed.get(missed.size() - 1).id();
			notificationService.findMissed(userId, lastSent)
					.forEach(notification -> registry.sendNotification(userId, emitter, notification));
		}
		return emitter;
	}

	private static Long parseLastEventId(String lastEventId) {
		if (!StringUtils.hasText(lastEventId)) {
			return null;
		}
		try {
			return Long.valueOf(lastEventId.trim());
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

}
