package com.cmc.hackathon4.domain.notification.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.cmc.hackathon4.domain.notification.converter.NotificationConverter;
import com.cmc.hackathon4.domain.notification.dto.NotificationCreatedEvent;
import com.cmc.hackathon4.domain.notification.dto.NotificationResponse;
import com.cmc.hackathon4.domain.notification.entity.Notification;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 다른 도메인이 알림을 만들 때 부른다. notification 은 다른 도메인 서비스를 호출하지 않는다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

	private static final int MAX_LIST_SIZE = 50;

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

	/** 내 알림 목록(최신순, 최대 50개). */
	public List<NotificationResponse> findMine(Long userId, boolean unreadOnly, Long afterId) {
		return notificationRepository
				.findMine(userId, afterId == null ? 0L : afterId, unreadOnly, PageRequest.ofSize(MAX_LIST_SIZE))
				.stream()
				.map(NotificationConverter::toResponse)
				.toList();
	}

	/** 받는 사람만 읽음 처리할 수 있다. 이미 읽었으면 그대로 성공. */
	@Transactional
	public void markRead(Long userId, Long notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
		if (!notification.getReceiver().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.NOTIFICATION_NOT_MINE);
		}
		notification.markRead(Instant.now());
	}

	/**
	 * 집에서 나간 사용자의 알림을 모두 지운다. 한 사용자는 한 집에만 속하므로 지금까지 받은 알림은 모두 나간 집의 것이다.
	 * 지우지 않으면 새 집의 홈·알림 목록·SSE 재연결에 이전 집 알림이 섞여 나온다.
	 */
	@Transactional
	public void deleteAllOf(Long receiverId) {
		notificationRepository.deleteAllByReceiverId(receiverId);
	}

	/** SSE 재연결용: lastEventId 보다 큰 내 알림을 오래된 순으로 (최대 50개). */
	public List<NotificationResponse> findMissed(Long userId, Long lastEventId) {
		return notificationRepository
				.findAllByReceiverIdAndIdGreaterThanOrderByIdAsc(userId, lastEventId, PageRequest.ofSize(MAX_LIST_SIZE))
				.stream()
				.map(NotificationConverter::toResponse)
				.toList();
	}

	/**
	 * 커밋 뒤 리스너용. 원래 트랜잭션은 이미 끝났으므로 새 읽기 트랜잭션에서 actor·transfer 와 함께 다시 조회해 변환한다.
	 */
	@Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
	public Optional<NotificationResponse> findResponseInNewTransaction(Long notificationId) {
		return notificationRepository.findWithActorAndTransferById(notificationId)
				.map(NotificationConverter::toResponse);
	}

}
