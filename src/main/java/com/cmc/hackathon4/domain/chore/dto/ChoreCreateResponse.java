package com.cmc.hackathon4.domain.chore.dto;

/** assignment 는 집이 ACTIVE 일 때만 채워지고, 아니면 null. */
public record ChoreCreateResponse(ChoreResponse chore, ChoreAssignmentResponse assignment) {
}
