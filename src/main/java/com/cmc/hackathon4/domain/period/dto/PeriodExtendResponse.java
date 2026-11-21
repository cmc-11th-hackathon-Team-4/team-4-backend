package com.cmc.hackathon4.domain.period.dto;

import java.time.LocalDate;

public record PeriodExtendResponse(Long periodId, LocalDate endDate) {
}
