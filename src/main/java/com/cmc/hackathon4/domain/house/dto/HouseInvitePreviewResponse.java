package com.cmc.hackathon4.domain.house.dto;

import com.cmc.hackathon4.domain.house.entity.HouseStatus;

public record HouseInvitePreviewResponse(String name, int memberCount, int maxMemberCount, String ownerName,
		HouseStatus status) {
}
