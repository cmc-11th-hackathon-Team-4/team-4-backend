package com.cmc.hackathon4.domain.notification.dto;

import java.time.Instant;

import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.user.entity.CharacterType;

/**
 * 알림 한 건(NotificationItem). 홈 조회, 알림 목록, SSE notification 이벤트가 같은 모양을 쓴다.
 * actor 는 시스템 알림이면 null, hearted 는 CHORE_RELIEVED 일 때만 값이 있다.
 * choreName 은 짐 이동과 연결된 알림(CHORE_RELIEVED, REQUEST_ACCEPTED, REQUEST_DECLINED, HEART_RECEIVED)에만 값이 있고 그 외에는 null 이다.
 */
public record NotificationResponse(Long id, NotificationType type, Actor actor, Long transferId, String choreName,
		String message, Boolean hearted, Instant readAt, Instant createdAt) {

	public record Actor(Long userId, String name, CharacterType characterType) {
	}

}
