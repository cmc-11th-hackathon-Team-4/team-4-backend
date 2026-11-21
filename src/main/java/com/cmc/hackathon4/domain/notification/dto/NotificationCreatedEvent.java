package com.cmc.hackathon4.domain.notification.dto;

/** 알림이 저장되면 발행된다. 실시간 전송은 커밋 뒤(AFTER_COMMIT)에 이 이벤트를 받아서 한다. */
public record NotificationCreatedEvent(Long notificationId, Long receiverId) {
}
