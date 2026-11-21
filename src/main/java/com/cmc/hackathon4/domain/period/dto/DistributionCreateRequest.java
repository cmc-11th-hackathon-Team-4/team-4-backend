package com.cmc.hackathon4.domain.period.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record DistributionCreateRequest(
		@NotNull(message = "결산 주기는 필수입니다.") @Schema(description = "결산 주기(일). 1, 3, 7 중 하나", example = "7", allowableValues = { "1", "3", "7" }) Integer settlementCycle,
		@NotNull(message = "시작일은 필수입니다.") @Schema(description = "시작일. 오늘(KST)부터 30일 뒤까지", example = "2026-12-01") LocalDate startDate) {
}
