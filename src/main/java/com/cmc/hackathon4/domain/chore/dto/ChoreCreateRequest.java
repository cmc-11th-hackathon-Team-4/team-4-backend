package com.cmc.hackathon4.domain.chore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChoreCreateRequest(
		@NotBlank(message = "집안일 이름은 필수입니다.") @Size(max = 20, message = "집안일 이름은 20자 이하여야 합니다.") String name) {
}
