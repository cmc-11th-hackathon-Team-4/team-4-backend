package com.cmc.hackathon4.domain.chore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChoreCreateRequest(
		@NotBlank(message = "집안일 이름은 필수입니다.") @Size(max = 100, message = "집안일 이름이 너무 깁니다.") @Schema(description = "앞뒤 공백·보이지 않는 문자를 빼고 연속 공백을 한 칸으로 줄인 뒤 1~20자", example = "화분 물주기") String name) {
}
