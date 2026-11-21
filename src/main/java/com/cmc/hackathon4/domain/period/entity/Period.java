package com.cmc.hackathon4.domain.period.entity;

import java.time.Instant;
import java.time.LocalDate;

import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 결산 기간(회차). 날짜는 KST 기준이며 endDate 24시(KST)에 끝난다. 집마다 READY·ACTIVE 는 각각 최대 1개. */
@Getter
@Entity
@Table(name = "period", uniqueConstraints = @UniqueConstraint(name = "uk_period_house_round", columnNames = {
		"house_id", "round"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Period extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "house_id", nullable = false)
	private House house;

	@Column(name = "round", nullable = false)
	private int round;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PeriodStatus status;

	@Column(name = "ended_at")
	private Instant endedAt;

	public Period(House house, int round, LocalDate startDate, LocalDate endDate) {
		this.house = house;
		this.round = round;
		this.startDate = startDate;
		this.endDate = endDate;
		this.status = PeriodStatus.READY;
	}

	/** READY 기간의 날짜를 다시 정한다(#15 를 다시 호출했을 때). */
	public void reschedule(LocalDate startDate, LocalDate endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
	}

	/** READY → ACTIVE */
	public void start() {
		this.status = PeriodStatus.ACTIVE;
	}

	public void extend(int days) {
		this.endDate = this.endDate.plusDays(days);
	}

	/** ACTIVE → ENDED */
	public void end(Instant endedAt) {
		this.status = PeriodStatus.ENDED;
		this.endedAt = endedAt;
	}

	public boolean isEnded() {
		return status == PeriodStatus.ENDED;
	}

	public boolean isActive() {
		return status == PeriodStatus.ACTIVE;
	}

}
