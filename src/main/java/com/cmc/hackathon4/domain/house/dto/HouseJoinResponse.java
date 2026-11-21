package com.cmc.hackathon4.domain.house.dto;

import com.cmc.hackathon4.domain.house.entity.HouseStatus;

public record HouseJoinResponse(Long id, String name, HouseStatus status) {
}
