package com.cmc.hackathon4.domain.user.dto;

import com.cmc.hackathon4.domain.user.entity.CharacterType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 보낸(null 이 아닌) 필드만 수정한다. */
public record UserUpdateRequest(
		@Size(min = 1, max = 10, message = "이름은 1~10자여야 합니다.") @Pattern(regexp = ".*\\S.*", message = "이름은 공백만 입력할 수 없습니다.") @Schema(example = "수아") String name,
		@Schema(example = "CHARACTER_2") CharacterType characterType) {
}
