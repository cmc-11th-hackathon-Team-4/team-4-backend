package com.cmc.hackathon4.domain.testitem.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestItemCreateRequest(
		@NotBlank(message = "제목은 필수입니다.") @Size(max = 100, message = "제목은 100자 이하여야 합니다.") @Schema(example = "테스트 제목") String title,
		@Size(max = 1000, message = "내용은 1000자 이하여야 합니다.") @Schema(example = "테스트 내용") String content) {
}
