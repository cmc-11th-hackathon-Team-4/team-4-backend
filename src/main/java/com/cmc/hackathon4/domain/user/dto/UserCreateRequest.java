package com.cmc.hackathon4.domain.user.dto;

import com.cmc.hackathon4.domain.user.entity.CharacterType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
		@NotBlank(message = "deviceId 는 필수입니다.") @Size(max = 64, message = "deviceId 는 64자 이하여야 합니다.") @Pattern(regexp = "^[!-~]+$", message = "deviceId 는 공백 없이 영문·숫자·기호만 쓸 수 있습니다.") @Schema(example = "6f1c2a9e-3b4d-4e8f-9a1b-2c3d4e5f6a7b") String deviceId,
		@NotBlank(message = "이름은 필수입니다.") @Schema(description = "1~10자. 앞뒤 공백은 빼고 저장하며 이모지도 한 글자로 센다.", example = "민지", maxLength = 10) String name,
		@NotNull(message = "캐릭터는 필수입니다.") @Schema(example = "CHARACTER_1") CharacterType characterType) {
}
