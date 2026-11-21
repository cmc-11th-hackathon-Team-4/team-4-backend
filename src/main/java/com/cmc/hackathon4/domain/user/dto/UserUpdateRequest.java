package com.cmc.hackathon4.domain.user.dto;

import com.cmc.hackathon4.domain.user.entity.CharacterType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

/** 보낸(null 이 아닌) 필드만 수정한다. */
public record UserUpdateRequest(
		@Pattern(regexp = ".*\\S.*", message = "이름은 공백만 입력할 수 없습니다.") @Schema(description = "1~10자. 앞뒤 공백은 빼고 저장하며 이모지도 한 글자로 센다.", example = "수아", maxLength = 10) String name,
		@Schema(example = "CHARACTER_2") CharacterType characterType) {
}
