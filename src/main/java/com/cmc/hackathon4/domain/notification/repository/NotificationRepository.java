package com.cmc.hackathon4.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import com.cmc.hackathon4.domain.notification.entity.Notification;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	/** 안 읽은 내 알림(최신순). actor, transfer 를 함께 가져온다. */
	@EntityGraph(attributePaths = {"actor", "transfer"})
	List<Notification> findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(Long receiverId);

	@EntityGraph(attributePaths = {"actor", "transfer"})
	Optional<Notification> findWithActorAndTransferById(Long id);

	/** 특정 이동에 걸린 내 안 읽은 알림 (하트를 보내면 CHORE_RELIEVED 알림을 읽음 처리) */
	List<Notification> findAllByReceiverIdAndTransferIdAndTypeAndReadAtIsNull(Long receiverId, Long transferId,
			NotificationType type);

}
