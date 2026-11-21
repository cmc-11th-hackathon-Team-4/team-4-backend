package com.cmc.hackathon4.domain.period.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record PeriodExtendRequest(@NotNull(message = "늘릴 일수는 필수입니다.") Integer days) {

	@JsonIgnore
	@AssertTrue(message = "늘릴 일수는 1, 3, 7 중 하나여야 합니다.")
	public boolean isAllowedDays() {
		return days == null || days == 1 || days == 3 || days == 7;
	}

}
