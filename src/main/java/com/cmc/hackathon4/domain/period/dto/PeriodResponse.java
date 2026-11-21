package com.cmc.hackathon4.domain.period.dto;

import java.time.LocalDate;

import com.cmc.hackathon4.domain.period.entity.PeriodStatus;

/** 기간 정보(id, round, startDate, endDate, status). 배분·시작·결산 응답이 함께 쓴다. */
public record PeriodResponse(Long id, int round, LocalDate startDate, LocalDate endDate, PeriodStatus status) {
}
