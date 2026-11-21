package com.cmc.hackathon4.domain.house.dto;

import com.cmc.hackathon4.domain.house.entity.SettlementCycle;

public record HouseUpdateResponse(Long id, String name, SettlementCycle settlementCycle) {
}
