package com.cmc.hackathon4.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import com.cmc.hackathon4.domain.notification.entity.Notification;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	/** 안 읽은 내 알림(최신순). actor, transfer 를 함께 가져온다. */
	@EntityGraph(attributePaths = {"actor", "transfer"})
	List<Notification> findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(Long receiverId);

	@EntityGraph(attributePaths = {"actor", "transfer"})
	Optional<Notification> findWithActorAndTransferById(Long id);

	/** 특정 이동에 걸린 내 안 읽은 알림 (하트를 보내면 CHORE_RELIEVED 알림을 읽음 처리) */
	List<Notification> findAllByReceiverIdAndTransferIdAndTypeAndReadAtIsNull(Long receiverId, Long transferId,
			NotificationType type);

	/** 내 알림 목록(최신순). unreadOnly 면 안 읽은 것만, afterId 보다 큰 id 만. 개수는 pageable 로 제한한다. */
	@EntityGraph(attributePaths = {"actor", "transfer"})
	@Query("select n from Notification n where n.receiver.id = :receiverId and n.id > :afterId "
			+ "and (:unreadOnly = false or n.readAt is null) order by n.id desc")
	List<Notification> findMine(@Param("receiverId") Long receiverId, @Param("afterId") Long afterId,
			@Param("unreadOnly") boolean unreadOnly, Pageable pageable);

	/** SSE 재연결 때 놓친 알림을 오래된 순으로 다시 보내기 위한 조회 (읽음 여부 무관). */
	@EntityGraph(attributePaths = {"actor", "transfer"})
	List<Notification> findAllByReceiverIdAndIdGreaterThanOrderByIdAsc(Long receiverId, Long id, Pageable pageable);

}
