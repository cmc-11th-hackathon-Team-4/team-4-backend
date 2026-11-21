package com.cmc.hackathon4.domain.house.entity;

import java.time.LocalDate;

public enum SettlementCycle {

	WEEKLY, BIWEEKLY, MONTHLY;

	/** 시작일을 포함한 기간의 마지막 날. MONTHLY 는 한 달 뒤 전날(예: 11/23 → 12/22). */
	public LocalDate endDateFrom(LocalDate startDate) {
		return switch (this) {
			case WEEKLY -> startDate.plusDays(6);
			case BIWEEKLY -> startDate.plusDays(13);
			case MONTHLY -> startDate.plusMonths(1).minusDays(1);
		};
	}

}
