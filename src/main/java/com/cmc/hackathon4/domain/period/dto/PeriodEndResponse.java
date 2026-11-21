package com.cmc.hackathon4.domain.period.dto;

import java.time.Instant;

import com.cmc.hackathon4.domain.period.entity.PeriodStatus;

public record PeriodEndResponse(Long periodId, PeriodStatus status, Instant endedAt) {
}
