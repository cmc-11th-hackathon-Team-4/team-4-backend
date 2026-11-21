package com.cmc.hackathon4.domain.user.converter;

import com.cmc.hackathon4.domain.user.dto.UserCreateRequest;
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

}
