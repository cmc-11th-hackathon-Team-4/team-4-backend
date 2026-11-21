package com.cmc.hackathon4.domain.notification.service;

import com.cmc.hackathon4.domain.notification.dto.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 알림 저장 트랜잭션이 커밋된 뒤에 SSE 로 실시간 전송한다. 어떤 실패도 저장 트랜잭션에 영향을 주지 않는다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

	private final NotificationService notificationService;
	private final NotificationEmitterRegistry registry;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onNotificationCreated(NotificationCreatedEvent event) {
		try {
			if (!registry.hasConnection(event.receiverId())) {
				return;
			}
			notificationService.findResponseInNewTransaction(event.notificationId())
					.ifPresent(response -> registry.sendNotification(event.receiverId(), response));
		}
		catch (Exception e) {
			log.warn("[NotificationSse] 실시간 전송 실패: notificationId={}, cause={}", event.notificationId(),
					e.getClass().getSimpleName());
		}
	}

}
