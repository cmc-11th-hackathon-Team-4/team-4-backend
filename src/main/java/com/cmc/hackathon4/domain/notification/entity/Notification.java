package com.cmc.hackathon4.domain.notification.entity;

import java.time.Instant;

import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 알림. "부탁 받음"은 여기 넣지 않고 transfer 의 PENDING 에서 뽑는다. */
@Getter
@Entity
@Table(name = "notification", indexes = @Index(name = "idx_notification_receiver_read", columnList = "receiver_id, read_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "receiver_id", nullable = false)
	private User receiver;

	// 알림을 일으킨 사람. 시스템 알림이면 null.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id")
	private User actor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transfer_id")
	private Transfer transfer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationType type;

	@Column(nullable = false, length = 100)
	private String message;

	// null 이면 안 읽음
	@Column(name = "read_at")
	private Instant readAt;

	public Notification(User receiver, User actor, Transfer transfer, NotificationType type, String message) {
		this.receiver = receiver;
		this.actor = actor;
		this.transfer = transfer;
		this.type = type;
		this.message = message;
	}

	/** 이미 읽었으면 처음 읽은 시각을 유지한다. */
	public void markRead(Instant now) {
		if (this.readAt == null) {
			this.readAt = now;
		}
	}

	public boolean isRead() {
		return readAt != null;
	}

}
