package com.cmc.hackathon4.domain.notification.service;

import java.util.Collection;

import com.cmc.hackathon4.domain.notification.dto.NotificationCreatedEvent;
import com.cmc.hackathon4.domain.notification.entity.Notification;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 다른 도메인이 알림을 만들 때 부른다. notification 은 다른 도메인 서비스를 호출하지 않는다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 알림을 저장한다. 호출한 쪽의 트랜잭션에 참여하며, 실시간 전송은 커밋 뒤에 이벤트로 처리된다.
	 * actor 는 알림을 일으킨 사람(시스템 알림이면 null), transfer 는 관련 짐 이동(없으면 null).
	 */
	@Transactional
	public Notification send(User receiver, User actor, Transfer transfer, NotificationType type, String message) {
		Notification notification = notificationRepository
				.save(new Notification(receiver, actor, transfer, type, message));
		eventPublisher.publishEvent(new NotificationCreatedEvent(notification.getId(), receiver.getId()));
		return notification;
	}

	/** 여러 사람에게 같은 알림을 보낸다. */
	@Transactional
	public void sendAll(Collection<User> receivers, User actor, Transfer transfer, NotificationType type,
			String message) {
		receivers.forEach(receiver -> send(receiver, actor, transfer, type, message));
	}

}
