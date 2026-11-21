package com.cmc.hackathon4.domain.period.dto;

import jakarta.validation.constraints.NotNull;

/** days 는 1, 3, 7 만 허용한다. 허용 값 검사는 PeriodService.extend 에서 한다. */
public record PeriodExtendRequest(@NotNull(message = "늘릴 일수는 필수입니다.") Integer days) {

}
