package com.cmc.hackathon4.domain.period.dto;

import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.CarryChoice;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CarryOverRequest(@NotEmpty(message = "이월 선택 목록은 비어 있을 수 없습니다.") @Valid List<@NotNull(message = "이월 선택은 필수입니다.") @Valid Choice> choices) {

	public record Choice(@NotNull(message = "짐 ID는 필수입니다.") @Schema(example = "1") Long assignmentId,
			@NotNull(message = "이월 선택은 KEEP 또는 RELEASE 여야 합니다.") @Schema(example = "KEEP") CarryChoice choice) {
	}

}
