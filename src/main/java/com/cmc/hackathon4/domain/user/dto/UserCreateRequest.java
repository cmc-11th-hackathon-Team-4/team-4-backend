package com.cmc.hackathon4.domain.user.dto;

import com.cmc.hackathon4.domain.user.entity.CharacterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
		@NotBlank(message = "deviceId 는 필수입니다.") @Size(max = 64, message = "deviceId 는 64자 이하여야 합니다.") String deviceId,
		@NotBlank(message = "이름은 필수입니다.") @Size(max = 10, message = "이름은 1~10자여야 합니다.") String name,
		@NotNull(message = "캐릭터는 필수입니다.") CharacterType characterType) {
}
