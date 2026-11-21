package com.cmc.hackathon4.domain.testredis.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TestRedisSaveRequest(
		@NotBlank(message = "값은 필수입니다.") @Size(max = 1000, message = "값은 1000자 이하여야 합니다.") String value,
		@Positive(message = "TTL 은 1초 이상이어야 합니다.") @Max(value = 31_536_000, message = "TTL 은 1년(31536000초) 이하여야 합니다.") Long ttlSeconds) {
}
