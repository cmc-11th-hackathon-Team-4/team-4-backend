package com.cmc.hackathon4.domain.chore.dto;

import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;

/** 집안일 추가로 만들어진 짐. */
public record ChoreAssignmentResponse(Long id, Long choreId, String name, AssignmentStatus status,
		boolean carriedOver) {
}
