package com.cmc.hackathon4.domain.chore.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ChoreBulkSaveRequest(
		@NotEmpty(message = "집안일을 1개 이상 골라 주세요.") @Size(max = 10, message = "집안일은 최대 10개까지 고를 수 있습니다.") @ArraySchema(schema = @Schema(description = "앞뒤 공백·보이지 않는 문자를 빼고 연속 공백을 한 칸으로 줄인 뒤 1~20자. 대소문자·악센트만 다른 이름은 중복", example = "설거지"), arraySchema = @Schema(example = "[\"설거지\", \"분리수거\", \"욕실 청소\"]")) List<@NotBlank(message = "집안일 이름은 필수입니다.") @Size(max = 100, message = "집안일 이름이 너무 깁니다.") String> names) {
}
