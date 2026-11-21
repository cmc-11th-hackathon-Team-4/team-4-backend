package com.cmc.hackathon4.domain.testredis;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TestRedisSaveRequest(
		@NotBlank(message = "값은 필수입니다.") @Size(max = 1000, message = "값은 1000자 이하여야 합니다.") String value,
		@Positive(message = "TTL 은 1초 이상이어야 합니다.") Long ttlSeconds) {
}
