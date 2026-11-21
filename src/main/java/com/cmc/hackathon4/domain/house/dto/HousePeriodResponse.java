package com.cmc.hackathon4.domain.house.dto;

import java.time.LocalDate;

import com.cmc.hackathon4.domain.period.entity.PeriodStatus;

public record HousePeriodResponse(Long id, int round, LocalDate startDate, LocalDate endDate, PeriodStatus status) {
}
