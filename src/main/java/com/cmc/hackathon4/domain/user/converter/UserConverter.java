package com.cmc.hackathon4.domain.user.converter;

import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.user.dto.UserCreateRequest;
import com.cmc.hackathon4.domain.user.dto.UserHouseResponse;
import com.cmc.hackathon4.domain.user.dto.UserMeResponse;
import com.cmc.hackathon4.domain.user.dto.UserResponse;
import com.cmc.hackathon4.domain.user.entity.User;

/** 요청 DTO → Entity, Entity → 응답 DTO 변환을 모아 둔다. */
public final class UserConverter {

	private UserConverter() {
	}

	public static User toEntity(UserCreateRequest request) {
		return new User(request.deviceId(), request.name(), request.characterType());
	}

	public static UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getCharacterType(), user.getMood());
	}

	/** membership 이 null 이면 소속 집이 없는 것으로 보고 house 를 null 로 둔다. */
	public static UserMeResponse toMeResponse(User user, HouseMember membership) {
		UserHouseResponse house = membership == null ? null
				: new UserHouseResponse(membership.getHouse().getId(), membership.getHouse().getName(),
						membership.getHouse().getStatus(), membership.getRole());
		return new UserMeResponse(user.getId(), user.getName(), user.getCharacterType(), user.getMood(), house);
	}

}
