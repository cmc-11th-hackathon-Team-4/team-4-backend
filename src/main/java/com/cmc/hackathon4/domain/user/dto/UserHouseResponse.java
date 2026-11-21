package com.cmc.hackathon4.domain.user.dto;

import com.cmc.hackathon4.domain.house.entity.HouseStatus;
import com.cmc.hackathon4.domain.house.entity.MemberRole;

public record UserHouseResponse(Long id, String name, HouseStatus status, MemberRole role) {
}
