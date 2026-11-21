package com.cmc.hackathon4.domain.assignment.dto;

import java.time.Instant;

import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;

/** 짐 완료 결과 */
public record AssignmentCompleteResponse(Long id, AssignmentStatus status, Instant completedAt) {
}
