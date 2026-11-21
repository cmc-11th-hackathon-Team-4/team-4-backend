package com.cmc.hackathon4.domain.testitem.dto;

import java.time.Instant;

public record TestItemResponse(Long id, String title, String content, Instant createdAt, Instant updatedAt) {
}
