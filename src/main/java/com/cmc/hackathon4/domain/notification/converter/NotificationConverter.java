package com.cmc.hackathon4.domain.notification.converter;

import com.cmc.hackathon4.domain.notification.dto.NotificationResponse;
import com.cmc.hackathon4.domain.notification.entity.Notification;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.user.entity.User;

/** Entity → 응답 DTO 변환을 모아 둔다. */
public final class NotificationConverter {

	private NotificationConverter() {
	}

	/** actor, transfer 를 읽으므로 트랜잭션 안에서(또는 함께 조회한 뒤) 호출한다. */
	public static NotificationResponse toResponse(Notification notification) {
		User actor = notification.getActor();
		Transfer transfer = notification.getTransfer();
		Boolean hearted = notification.getType() == NotificationType.CHORE_RELIEVED && transfer != null
				? transfer.isHearted()
				: null;
		return new NotificationResponse(
				notification.getId(),
				notification.getType(),
				actor == null ? null
						: new NotificationResponse.Actor(actor.getId(), actor.getName(), actor.getCharacterType()),
				transfer == null ? null : transfer.getId(),
				transfer == null ? null : transfer.getAssignment().getChore().getName(),
				notification.getMessage(),
				hearted,
				notification.getReadAt(),
				notification.getCreatedAt());
	}

}
