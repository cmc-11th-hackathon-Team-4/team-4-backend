package com.cmc.hackathon4.domain.house.dto;

import com.cmc.hackathon4.domain.house.entity.HouseStatus;

public record HouseCreateResponse(Long id, String name, String inviteCode, HouseStatus status) {
}
