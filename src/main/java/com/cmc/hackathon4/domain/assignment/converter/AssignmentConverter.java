package com.cmc.hackathon4.domain.assignment.converter;

import com.cmc.hackathon4.domain.assignment.dto.AssignmentCompleteResponse;
import com.cmc.hackathon4.domain.assignment.entity.Assignment;

/** Entity → 응답 DTO 변환을 모아 둔다. */
public final class AssignmentConverter {

	private AssignmentConverter() {
	}

	public static AssignmentCompleteResponse toResponse(Assignment assignment) {
		return new AssignmentCompleteResponse(assignment.getId(), assignment.getStatus(),
				assignment.getCompletedAt());
	}

}
