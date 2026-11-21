package com.cmc.hackathon4.domain.chore.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ChoreBulkSaveRequest(
		@NotEmpty(message = "집안일을 1개 이상 골라 주세요.") @Size(max = 10, message = "집안일은 최대 10개까지 고를 수 있습니다.") List<@NotBlank(message = "집안일 이름은 필수입니다.") @Size(max = 20, message = "집안일 이름은 20자 이하여야 합니다.") String> names) {
}
