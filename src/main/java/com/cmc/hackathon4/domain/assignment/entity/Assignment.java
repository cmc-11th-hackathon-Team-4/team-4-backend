package com.cmc.hackathon4.domain.assignment.entity;

import java.time.Instant;

import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.period.entity.Period;
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

/** 짐: 한 기간에 한 사람이 맡은 집안일 하나. */
@Getter
@Entity
@Table(name = "assignment", indexes = @Index(name = "idx_assignment_period_assignee", columnList = "period_id, assignee_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "period_id", nullable = false)
	private Period period;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "chore_id", nullable = false)
	private Chore chore;

	// 현재 담당자. 덜어가기·부탁 수락으로 바뀐다.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assignee_id", nullable = false)
	private User assignee;

	// 최초 배분 담당자. 바뀌지 않는다.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "original_assignee_id", nullable = false)
	private User originalAssignee;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AssignmentStatus status;

	// 직전 기간에서 이월(KEEP)되어 고정된 짐인지
	@Column(name = "carried_over", nullable = false)
	private boolean carriedOver;

	@Column(name = "completed_at")
	private Instant completedAt;

	// 결산에서 못 한 일을 어떻게 할지. null 은 KEEP 으로 취급한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "carry_choice", length = 20)
	private CarryChoice carryChoice;

	// 집안일이 삭제되어 담당자의 짐에서 빠진 시각. null 이 아니면 삭제된 짐이다.
	@Column(name = "deleted_at")
	private Instant deletedAt;

	public Assignment(Period period, Chore chore, User assignee, boolean carriedOver) {
		this.period = period;
		this.chore = chore;
		this.assignee = assignee;
		this.originalAssignee = assignee;
		this.status = AssignmentStatus.TODO;
		this.carriedOver = carriedOver;
	}

	public void complete(Instant completedAt) {
		this.status = AssignmentStatus.DONE;
		this.completedAt = completedAt;
	}

	public void delete(Instant now) {
		this.deletedAt = now;
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public void changeAssignee(User assignee) {
		this.assignee = assignee;
	}

	public void chooseCarry(CarryChoice carryChoice) {
		this.carryChoice = carryChoice;
	}

	public boolean isDone() {
		return status == AssignmentStatus.DONE;
	}

	/** 못 한 일을 다음 기간에도 같은 담당자가 맡는지 (미선택 포함) */
	public boolean isKeptForNextPeriod() {
		return status == AssignmentStatus.TODO && carryChoice != CarryChoice.RELEASE;
	}

}
