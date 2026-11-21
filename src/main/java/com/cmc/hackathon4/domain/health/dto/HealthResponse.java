package com.cmc.hackathon4.domain.health.dto;

import java.time.Instant;

public record HealthResponse(String status, Instant serverTime) {
}
