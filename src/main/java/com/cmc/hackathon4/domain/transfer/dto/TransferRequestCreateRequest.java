package com.cmc.hackathon4.domain.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 짐 올리기 부탁 요청 */
public record TransferRequestCreateRequest(
		@Schema(description = "부탁 받을 사용자 ID (같은 집의 다른 구성원)", example = "2") @NotNull(message = "부탁 받을 사용자를 입력해주세요.") Long toUserId) {
}
