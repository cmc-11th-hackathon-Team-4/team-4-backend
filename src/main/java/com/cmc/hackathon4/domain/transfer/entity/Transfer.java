package com.cmc.hackathon4.domain.transfer.entity;

import java.time.Instant;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
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

/** 짐 이동. fromUser 는 짐을 넘기는 사람(원래 담당자), toUser 는 받는 사람. */
@Getter
@Entity
@Table(name = "transfer", indexes = @Index(name = "idx_transfer_to_user_status", columnList = "to_user_id, status"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assignment_id", nullable = false)
	private Assignment assignment;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TransferType type;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "from_user_id", nullable = false)
	private User fromUser;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "to_user_id", nullable = false)
	private User toUser;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TransferStatus status;

	@Column(name = "responded_at")
	private Instant respondedAt;

	@Column(name = "hearted_at")
	private Instant heartedAt;

	private Transfer(Assignment assignment, TransferType type, User fromUser, User toUser, TransferStatus status,
			Instant respondedAt) {
		this.assignment = assignment;
		this.type = type;
		this.fromUser = fromUser;
		this.toUser = toUser;
		this.status = status;
		this.respondedAt = respondedAt;
	}

	/** 짐 올리기 부탁: 담당자(fromUser)가 toUser 에게 부탁한다. PENDING 으로 시작. */
	public static Transfer request(Assignment assignment, User fromUser, User toUser) {
		return new Transfer(assignment, TransferType.REQUEST, fromUser, toUser, TransferStatus.PENDING, null);
	}

	/** 덜어가기: toUser 가 fromUser 의 짐을 가져간다. 승인 없이 바로 ACCEPTED. */
	public static Transfer relieve(Assignment assignment, User fromUser, User toUser, Instant now) {
		return new Transfer(assignment, TransferType.RELIEVE, fromUser, toUser, TransferStatus.ACCEPTED, now);
	}

	public void accept(Instant now) {
		this.status = TransferStatus.ACCEPTED;
		this.respondedAt = now;
	}

	public void decline(Instant now) {
		this.status = TransferStatus.DECLINED;
		this.respondedAt = now;
	}

	public void cancel(Instant now) {
		this.status = TransferStatus.CANCELED;
		this.respondedAt = now;
	}

	public void heart(Instant now) {
		this.heartedAt = now;
	}

	public boolean isPending() {
		return status == TransferStatus.PENDING;
	}

	public boolean isHearted() {
		return heartedAt != null;
	}

}
