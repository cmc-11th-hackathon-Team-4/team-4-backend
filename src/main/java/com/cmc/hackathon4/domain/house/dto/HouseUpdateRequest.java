package com.cmc.hackathon4.domain.house.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 보낸 필드만 수정한다. null 이면 바꾸지 않는다. */
public record HouseUpdateRequest(
		@Size(max = 15, message = "집 이름은 15자 이하여야 합니다.") @Pattern(regexp = ".*\\S.*", message = "집 이름은 공백만으로 구성할 수 없습니다.") @Schema(example = "새 우리집") String name,
		@Schema(description = "결산 주기(일). 1, 3, 7 중 하나", example = "3", allowableValues = { "1", "3", "7" }) Integer settlementCycle) {
}
