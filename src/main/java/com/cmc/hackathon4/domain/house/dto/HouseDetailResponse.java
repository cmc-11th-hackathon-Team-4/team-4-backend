package com.cmc.hackathon4.domain.house.dto;

import java.util.List;

import com.cmc.hackathon4.domain.house.entity.HouseStatus;
import com.cmc.hackathon4.domain.house.entity.SettlementCycle;
import com.cmc.hackathon4.domain.period.dto.PeriodResponse;

public record HouseDetailResponse(Long id, String name, String inviteCode, HouseStatus status,
		SettlementCycle settlementCycle, int memberCount, int maxMemberCount, int choreCount,
		List<HouseMemberResponse> members, PeriodResponse currentPeriod) {
}
