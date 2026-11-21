package com.cmc.hackathon4.domain.period.dto;

import java.time.LocalDate;

import com.cmc.hackathon4.domain.house.entity.SettlementCycle;
import jakarta.validation.constraints.NotNull;

public record DistributionCreateRequest(
		@NotNull(message = "결산 주기는 필수입니다.") SettlementCycle settlementCycle,
		@NotNull(message = "시작일은 필수입니다.") LocalDate startDate) {
}
