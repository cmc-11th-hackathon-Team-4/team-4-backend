package com.cmc.hackathon4.domain.period.dto;

import com.cmc.hackathon4.domain.house.entity.HouseStatus;

/** #16 배분 확정·우리 집 시작하기 응답. */
public record DistributionStartResponse(Long houseId, HouseStatus status, PeriodResponse period) {
}
