package com.cmc.hackathon4.domain.house.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HouseCreateRequest(
		@NotBlank(message = "집 이름은 필수입니다.") @Size(max = 15, message = "집 이름은 15자 이하여야 합니다.") String name) {
}
