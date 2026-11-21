package com.cmc.hackathon4.domain.house.converter;

import com.cmc.hackathon4.domain.house.dto.HouseCreateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseCreateResponse;
import com.cmc.hackathon4.domain.house.entity.House;

public final class HouseConverter {

	private HouseConverter() {
	}

	public static House toEntity(HouseCreateRequest request, String inviteCode) {
		return new House(request.name().trim(), inviteCode);
	}

	public static HouseCreateResponse toCreateResponse(House house) {
		return new HouseCreateResponse(house.getId(), house.getName(), house.getInviteCode(), house.getStatus());
	}

}
